## Run
```bash
mvn package azure-functions:package azure-functions:run -pl samples/server/petstore/kotlin-azure-function-app -P default,azure -DenableDebug
```

## CosmosDB

See [install-certs](https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator-export-ssl-certificates#export-emulator-certificate)

Go to https://localhost:8081/_explorer/index.html

### Linux / Arch

#### Run
```bash
ipaddr="`ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}' | head -n 1`"

docker run --rm \
    --publish 8081:8081 \
    --publish 10251-10254:10251-10254 \
    --memory 3g --cpus=2.0 \
    --name=test-linux-emulator \
    --env AZURE_COSMOS_EMULATOR_PARTITION_COUNT=10 \
    --env AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE=true \
    --env AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE=$ipaddr \
    --env AZURE_COSMOS_EMULATOR_CERTIFICATE=/usr/local/bin/cosmos/default.sslcert.pfx \
    -v $(pwd)/samples/server/petstore/kotlin-azure-function-app/certs/default.sslcert.pfx:/usr/local/bin/cosmos/default.sslcert.pfx \
    --interactive \
    --tty \
    mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator
```
#### Install cert
```bash
sudo cp $(pwd)/samples/server/petstore/kotlin-azure-function-app/certs/cosmosdb.crt /etc/ssl/certs/
```
