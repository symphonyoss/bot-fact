# Symphony Eliza Bot

Eliza is simple fact telling bot. You will interact with Eliza by starting the message with "Eliza". She will tell you some facts from her amazing atabase.
Business usage: 

1. We can replace the fun fact to company fact so employee can get answer about the company. \
2. HR related facts.

The java command runs and exit, there is no daemon running and waiting for incoming messages.


## Example
```

git clone https://github.com/kinkoi/eliza.git
cd eliza
mvn clean package

java \
-Dagent.url=https://corporate-api.symphony.com:8444/agent \
-Dbot.user.email=bot.user13@symphony.com \
-Dbot.user.name=bot.user13 \
-Dcerts.dir=/Users/kinkoi.lo/dev/bot/ \
-Dkeyauth.url=https://corporate-api.symphony.com:8444/keyauth \
-Dkeystore.password=XXXX \
-Dpod.url=https://corporate-api.symphony.com:8444/pod \
-Dsessionauth.url=https://corporate-api.symphony.com:8444/sessionauth \
-Dtruststore.file=/Users/kinkoi.lo/dev/bot/truststore.ts \
-Dtruststore.password=XXXX -Droom.stream=ROOM_ID \
-Dbot.user.email=bot.user13@symphony.com

```

## Libraries
- [Symphony Java Client](https://github.com/symphonyoss/symphony-java-client)

