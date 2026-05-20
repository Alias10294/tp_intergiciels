@echo off
setlocal

if "%~1"=="" (
    echo Usage: start_chatting.bat ^<ClientName^> [GroupId]
    echo Example:
    echo   start_chatting.bat ClientA
    echo   start_chatting.bat ClientA ClientA_Group
    exit /b 1
)

set "CLIENT_NAME=%~1"

if "%~2"=="" (
    set "GROUP_ID=%CLIENT_NAME%_Group"
) else (
    set "GROUP_ID=%~2"
)

set "APPLICATION_MONNOM=%CLIENT_NAME%"
set "SPRING_KAFKA_CONSUMER_GROUP_ID=%GROUP_ID%"

echo Starting client:
echo   name    = %CLIENT_NAME%
echo   groupId = %GROUP_ID%
echo.

java -jar "client-shell\target\shellClient-0.0.1.jar"

endlocal