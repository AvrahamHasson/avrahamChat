package tcp

import (
	"encoding/binary"
	"io"
	"net"
)

func WritePacket(conn net.Conn, uuid string, targetName string, messageBody []byte) error {
    uuidBytes := []byte(uuid)
    if err := binary.Write(conn, binary.BigEndian, int32(len(uuidBytes))); err != nil {
        return err
    }
    if _, err := conn.Write(uuidBytes); err != nil {
        return err
    }

    targetNameBytes := []byte(targetName)
    if err := binary.Write(conn, binary.BigEndian, int32(len(targetNameBytes))); err != nil {
        return err
    }
    if _, err := conn.Write(targetNameBytes); err != nil {
        return err
    }

    if err := binary.Write(conn, binary.BigEndian, int32(len(messageBody))); err != nil {
        return err
    }
    if _, err := conn.Write(messageBody); err != nil {
        return err
    }

    return nil
}

func ReadPacket(conn net.Conn) (string, string, []byte, error) {
    var uuidSize int32
    if err := binary.Read(conn, binary.BigEndian, &uuidSize); err != nil {
        return "", "", nil, err
    }
    uuidBuf := make([]byte, uuidSize)
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

    return string(uuidBuf), string(nameBuf), bodyBuf, nil
}
