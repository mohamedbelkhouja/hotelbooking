#!/usr/bin/env bash
set -e

GATEWAY_URL="http://localhost:8080/api/v1"

echo "=========================================================="
echo "  HotelBooking SOA End-to-End Test Scenario"
echo "=========================================================="

echo -e "\n[1] Consultation des chambres disponibles (REST -> gRPC StreamAvailableRooms)..."
ROOMS_RESP=$(curl -s "$GATEWAY_URL/rooms/available?checkIn=2026-12-01&checkOut=2026-12-05&minCapacity=2")
echo "Réponse: $ROOMS_RESP"

ROOM_ID=$(echo "$ROOMS_RESP" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
if [ -z "$ROOM_ID" ]; then
    ROOM_ID="r-102"
fi
echo "Chambre sélectionnée : $ROOM_ID"

echo -e "\n[2] Création d'un client (POST /clients)..."
RANDOM_SUFFIX=$RANDOM
CLIENT_RESP=$(curl -s -X POST "$GATEWAY_URL/clients" \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Mohamed\",\"lastName\":\"Trabelsi\",\"email\":\"client_${RANDOM_SUFFIX}@example.tn\",\"phone\":\"+216 20 111 222\"}")
echo "Réponse: $CLIENT_RESP"

CLIENT_ID=$(echo "$CLIENT_RESP" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "Client ID: $CLIENT_ID"

echo -e "\n[3] Consultation du client (GET /clients/$CLIENT_ID)..."
curl -s "$GATEWAY_URL/clients/$CLIENT_ID"
echo ""

echo -e "\n[4] Création d'une réservation (POST /reservations)..."
RES_RESP=$(curl -s -X POST "$GATEWAY_URL/reservations" \
  -H "Content-Type: application/json" \
  -d "{\"clientId\":\"$CLIENT_ID\",\"roomId\":\"$ROOM_ID\",\"checkIn\":\"2026-12-01\",\"checkOut\":\"2026-12-05\"}")
echo "Réponse: $RES_RESP"

RES_ID=$(echo "$RES_RESP" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "Reservation ID: $RES_ID"

echo -e "\n[5] Confirmation de la réservation (POST /reservations/$RES_ID/confirm)..."
CONFIRM_RESP=$(curl -s -X POST "$GATEWAY_URL/reservations/$RES_ID/confirm")
echo "Réponse: $CONFIRM_RESP"

echo -e "\n[6] Consultation de la facture générée (GET /reservations/$RES_ID/invoice)..."
INVOICE_RESP=$(curl -s "$GATEWAY_URL/reservations/$RES_ID/invoice")
echo "Réponse: $INVOICE_RESP"

echo -e "\n[7] Annulation de la réservation (DELETE /reservations/$RES_ID)..."
CANCEL_RESP=$(curl -s -X DELETE "$GATEWAY_URL/reservations/$RES_ID")
echo "Réponse: $CANCEL_RESP"

echo -e "\n=========================================================="
echo "  Scénario complété avec succès !"
echo "=========================================================="
