package models

type InitPayload struct {
	Username string `json:"username"`
	TcpPort  string `json:"tcpPort"`
	Action   string `json:"action"`
}