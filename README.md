# ExchangeRateAPI

An app to convert between currencies, built using HTTP4S

## Running the app

To start the app simply do `sbt run`, it will start on port `8080` (specified in main)

## Calling the app

To call the app do 

```bash
curl -X POST \
     -d '{"fromCurrency":"GBP", "toCurrency":"EUR", "amount":110}' \
     http://localhost:8080/api/convert
```

The app should return a body like
```{"exchange":1.1842027355,"amount":130.262,"original":110}```

## Running the tests

This is done with `sbt test`

