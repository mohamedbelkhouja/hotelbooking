Set-Location $PSScriptRoot
cmd /c "docker compose up --build 2>&1" | Tee-Object -FilePath "$PSScriptRoot\docker-result.txt"
"EXIT_CODE=$LASTEXITCODE" | Out-File -Append "$PSScriptRoot\docker-result.txt"
