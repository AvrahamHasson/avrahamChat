package manager

import (
	"encoding/json"
	"fmt"
	"log"
	"go-tcp-medium/models" 
	"go-tcp-medium/tcp"
	"net"
	"sync"
	"time"

	"github.com/streadway/amqp"
)

var (
	activeInstances = make(map[string]chan bool)
	instancesMu     sync.Mutex
)

func HandleInit(init *models.InitPayload, conn *amqp.Connection) {
	instancesMu.Lock()
	defer instancesMu.Unlock()

	if stopChan, exists := activeInstances[init.TcpPort]; exists {
		stopChan <- true
		delete(activeInstances, init.TcpPort)
		time.Sleep(200 * time.Millisecond) 
	}

	if init.Action == "LOGOUT" {
		fmt.Printf("User %s logged out. Port %s released.\n", init.Username, init.TcpPort)
		return
	}

	stopChan := make(chan bool, 1)
	activeInstances[init.TcpPort] = stopChan
	go runUserInstance(init.Username, init.TcpPort, conn, stopChan)
	fmt.Printf("Instance LIVE: %s on port %s\n", init.Username, init.TcpPort)
}

func runUserInstance(username string, port string, conn *amqp.Connection, stopChan chan bool) {
	ch, err := conn.Channel()
	if err != nil {
		log.Printf("[%s] Channel Error: %v", username, err)
		return
	}
	defer ch.Close()

	outQ := fmt.Sprintf("go_outgoing_%s", username)
	inQ := fmt.Sprintf("go_incoming_%s", username)
	statQ := fmt.Sprintf("go_status_%s", username)

	for _, q := range []string{outQ, inQ, statQ} {
		ch.QueueDeclare(q, false, false, false, false, nil)
	}

	ln, err := net.Listen("tcp", ":"+port)
	if err != nil {
		log.Printf("[%s] Port %s is busy: %v", username, port, err)
		return
	}

	go func() {
		<-stopChan
		ln.Close()
	}()

	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				return 
			}
			go handleIncomingTCP(conn, ch, inQ)
		}
	}()

	msgs, _ := ch.Consume(outQ, "", true, false, false, false, nil)
	for {
		select {
		case <-stopChan:
			return
		case d, ok := <-msgs:
			if !ok {
				return
			}

			var payload models.RabbitPayload
			if err := json.Unmarshal(d.Body, &payload); err != nil {
				log.Printf("Unmarshal error: %v", err)
				continue
			}

			go func(p models.RabbitPayload) {
				if success := sendTCP(&p); success {
					statusUpdate, _ := json.Marshal(map[string]string{
						"uuid":   p.UUID,
						"status": "delivered",
					})
					ch.Publish("", statQ, false, false, amqp.Publishing{
						ContentType: "application/json",
						Body:        statusUpdate,
					})
				}
			}(payload)
		}
	}
}

func sendTCP(p *models.RabbitPayload) bool {
	conn, err := net.DialTimeout("tcp", p.TargetAddress, 5*time.Second)
	if err != nil {
		log.Printf("Failed to connect to %s: %v", p.TargetAddress, err)
		return false
	}
	defer conn.Close()

	if err := tcp.WritePacket(conn, p.UUID, p.TargetUsername, p.Message); err != nil {
		log.Printf("Write error: %v", err)
		return false
	}

	ackBuf := make([]byte, 3)
	conn.SetReadDeadline(time.Now().Add(2 * time.Second))
	n, err := conn.Read(ackBuf)
	return err == nil && string(ackBuf[:n]) == "ACK"
}

func handleIncomingTCP(conn net.Conn, ch *amqp.Channel, inQ string) {
	defer conn.Close()

	uuid, target, body, err := tcp.ReadPacket(conn)
	if err != nil {
		return
	}

	forwardPayload := map[string]interface{}{
		"targetUsername": target,
		"message":        json.RawMessage(body),
	}
	
	finalJson, _ := json.Marshal(forwardPayload)

	err = ch.Publish("", inQ, false, false, amqp.Publishing{
		ContentType: "application/json",
		Body:        finalJson,
	})

	if err == nil {
		conn.Write([]byte("ACK"))
		fmt.Printf("-> Forwarded msg to Rabbit: %s (UUID: %s)\n", target, uuid)
	}
}