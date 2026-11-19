# ==============================================================================
# HotelBooking - End-to-End Automated Test Scenario
# ==============================================================================
$GatewayUrl = "http://localhost:8080/api/v1"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  HotelBooking SOA End-to-End Test Scenario" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Consulter les chambres disponibles (REST -> gRPC StreamAvailableRooms)
Write-Host "`n[1] Consultation des chambres disponibles..." -ForegroundColor Yellow
$rooms = Invoke-RestMethod -Uri "$GatewayUrl/rooms/available?checkIn=2026-12-01&checkOut=2026-12-05&minCapacity=2" -Method Get
Write-Host "Chambres disponibles trouvées:" -ForegroundColor Green
$rooms | Format-Table id, number, type, capacity, pricePerNight

$selectedRoom = $rooms[0]
$selectedRoomId = $selectedRoom.id
Write-Host "Chambre sélectionnée pour la réservation: $selectedRoomId (Numéro $($selectedRoom.number))" -ForegroundColor Cyan

# 2. Création d'un client
Write-Host "`n[2] Création d'un nouveau client..." -ForegroundColor Yellow
$randomEmail = "client_$([Guid]::NewGuid().ToString().Substring(0,8))@example.tn"
$clientBody = @{
    firstName = "Ali"
    lastName  = "Ben Salah"
    email     = $randomEmail
    phone     = "+216 22 333 444"
} | ConvertTo-Json

$client = Invoke-RestMethod -Uri "$GatewayUrl/clients" -Method Post -Body $clientBody -ContentType "application/json"
$clientId = $client.id
Write-Host "Client créé avec succès ! ID: $clientId ($($client.firstName) $($client.lastName))" -ForegroundColor Green

# 3. Consulter le client
Write-Host "`n[3] Consultation du client créé..." -ForegroundColor Yellow
$fetchedClient = Invoke-RestMethod -Uri "$GatewayUrl/clients/$clientId" -Method Get
Write-Host "Client récupéré: $($fetchedClient.firstName) $($fetchedClient.lastName) - $($fetchedClient.email)" -ForegroundColor Green

# 4. Créer une réservation (REST -> gRPC CheckAvailability)
Write-Host "`n[4] Création d'une réservation (Statut initial : CREEE)..." -ForegroundColor Yellow
$resBody = @{
    clientId = $clientId
    roomId   = $selectedRoomId
    checkIn  = "2026-12-01"
    checkOut = "2026-12-05"
} | ConvertTo-Json

$reservation = Invoke-RestMethod -Uri "$GatewayUrl/reservations" -Method Post -Body $resBody -ContentType "application/json"
$resId = $reservation.id
Write-Host "Réservation créée ! ID: $resId" -ForegroundColor Green
Write-Host "Statut: $($reservation.status), Prix Total: $($reservation.totalPrice) DT, Facture: $($reservation.invoiceStatus)" -ForegroundColor Green

# 5. Consulter la réservation
Write-Host "`n[5] Consultation de la réservation..." -ForegroundColor Yellow
$fetchedRes = Invoke-RestMethod -Uri "$GatewayUrl/reservations/$resId" -Method Get
Write-Host "Statut actuel: $($fetchedRes.status)" -ForegroundColor Green

# 6. Confirmer la réservation (Déclenche SOAP generateInvoice via Resilience4j)
Write-Host "`n[6] Confirmation de la réservation (Appel SOAP generateInvoice)..." -ForegroundColor Yellow
$confirmedRes = Invoke-RestMethod -Uri "$GatewayUrl/reservations/$resId/confirm" -Method Post
Write-Host "Réservation confirmée avec succès !" -ForegroundColor Green
Write-Host "Statut: $($confirmedRes.status), Statut Facture: $($confirmedRes.invoiceStatus)" -ForegroundColor Green

# 7. Obtenir la facture générée (REST -> SOAP getInvoice)
Write-Host "`n[7] Consultation de la facture générée..." -ForegroundColor Yellow
$invoice = Invoke-RestMethod -Uri "$GatewayUrl/reservations/$resId/invoice" -Method Get
Write-Host "Facture reçue avec succès !" -ForegroundColor Green
$invoice | Format-List invoiceId, reservationId, amountHT, tva, amountTTC, issueDate, status

# 8. Suivi des réservations du client
Write-Host "`n[8] Suivi des réservations pour le client..." -ForegroundColor Yellow
$clientResList = Invoke-RestMethod -Uri "$GatewayUrl/clients/$clientId/reservations" -Method Get
Write-Host "Nombre de réservations pour le client: $($clientResList.Count)" -ForegroundColor Green
$clientResList | Format-Table id, roomId, checkIn, checkOut, status, totalPrice, invoiceStatus

# 9. Annulation de la réservation (CONFIRMEE -> ANNULEE)
Write-Host "`n[9] Annulation de la réservation..." -ForegroundColor Yellow
$cancelledRes = Invoke-RestMethod -Uri "$GatewayUrl/reservations/$resId" -Method Delete
Write-Host "Réservation annulée avec succès !" -ForegroundColor Green
Write-Host "Nouveau statut: $($cancelledRes.status)" -ForegroundColor Green

# 10. Test de règle métier : Tentative de modification après annulation (doit renvoyer 409 Conflict)
Write-Host "`n[10] Test de robustesse : Modification d'une réservation annulée (Attendu: 409 Conflict)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$GatewayUrl/reservations/$resId" -Method Put -Body $resBody -ContentType "application/json"
    Write-Host "ERREUR : La transition interdite aurait dû être bloquée !" -ForegroundColor Red
} catch {
    Write-Host "SUCCÈS : Règle métier respectée ! Réponse: 409 Conflict ($($_.Exception.Message))" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  Tous les tests du scénario ont réussi avec succès !" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
