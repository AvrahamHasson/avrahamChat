package models

import "encoding/json"

type RabbitPayload struct {
	TargetAddress  string          `json:"targetAddress"`
	TargetUsername string          `json:"targetUsername"`
	UUID           string          `json:"uuid"`
	Message        json.RawMessage `json:"message"`
}