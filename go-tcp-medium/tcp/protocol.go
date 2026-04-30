package tcp

import (
	"encoding/base64"
	"encoding/binary"
	"io"
	"net"
)

func WritePacket(conn net.Conn, uuid string, targetName string, messageBody []byte) error {
	if _, err := conn.Write([]byte(uuid)); err != nil {
		return err
	}

	targetNameBytes := []byte(targetName)
	if err := binary.Write(conn, binary.BigEndian, int32(len(targetNameBytes))); err != nil {
		return err
	}
	if _, err := conn.Write(targetNameBytes); err != nil {
		return err
	}

	encodedBody := base64.StdEncoding.EncodeToString(messageBody)
	if err := binary.Write(conn, binary.BigEndian, int32(len(encodedBody))); err != nil {
		return err
	}
	if _, err := conn.Write([]byte(encodedBody)); err != nil {
		return err
	}

	return nil
}

func ReadPacket(conn net.Conn) (string, string, []byte, error) {
	uuidBuf := make([]byte, 36)
	if _, err := io.ReadFull(conn, uuidBuf); err != nil {
		return "", "", nil, err
	}

	var nameSize int32
	if err := binary.Read(conn, binary.BigEndian, &nameSize); err != nil {
		return "", "", nil, err
	}
	nameBuf := make([]byte, nameSize)
	if _, err := io.ReadFull(conn, nameBuf); err != nil {
		return "", "", nil, err
	}

	var bodySize int32
	if err := binary.Read(conn, binary.BigEndian, &bodySize); err != nil {
		return "", "", nil, err
	}
	bodyBuf := make([]byte, bodySize)
	if _, err := io.ReadFull(conn, bodyBuf); err != nil {
		return "", "", nil, err
	}

	decoded, err := base64.StdEncoding.DecodeString(string(bodyBuf))
	return string(uuidBuf), string(nameBuf), decoded, err
}
