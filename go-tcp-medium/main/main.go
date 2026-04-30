package main

import (
	"encoding/json"
	"fmt"
	"log"
	"go-tcp-medium/models"
    "go-tcp-medium/manager"

	"github.com/streadway/amqp"
)

func main() {
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	if err != nil {
		log.Fatalf("Failed to connect to RabbitMQ: %v", err)
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("Failed to open a channel: %v", err)
	}
	defer ch.Close()

	initQueue, err := ch.QueueDeclare("go_init", false, false, false, false, nil)
	if err != nil {
		log.Fatalf("Failed to declare init queue: %v", err)
	}

	msgs, err := ch.Consume(initQueue.Name, "", true, false, false, false, nil)
	if err != nil {
		log.Fatalf("Failed to start consuming: %v", err)
	}

	fmt.Println("Medium Service is LIVE | Waiting for init from Kotlin...")

	for d := range msgs {
		var init models.InitPayload
		if err := json.Unmarshal(d.Body, &init); err != nil {
			log.Printf("Error unmarshaling init payload: %v", err)
			continue
		}

		manager.HandleInit(&init, conn)
	}
}