$MVN = "C:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"
Set-Location $PSScriptRoot
& $MVN clean package "-DskipTests" "-B" "--no-transfer-progress" 2>&1 | Tee-Object -FilePath "$PSScriptRoot\build-result.txt"
"EXIT_CODE=$LASTEXITCODE" | Out-File -Append "$PSScriptRoot\build-result.txt"
