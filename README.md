# Macrocosm
[![wakatime](https://wakatime.com/badge/user/4f3de2e1-f9cb-4480-9047-74ecccf9f9c0/project/9b9055db-2125-49ba-ab9e-ae424a54a0af.svg)](https://wakatime.com/badge/user/4f3de2e1-f9cb-4480-9047-74ecccf9f9c0/project/9b9055db-2125-49ba-ab9e-ae424a54a0af)

🎉 Open source 🎉

# Completion Status
Macroocsm 2.0 is still in development. If you want to try out current features of Macrocosm you can download the plugin and check it yourself.

## Running Macrocosm locally
It's really not that hard.


### Prerequisites
1. Java 17
2. Paper 1.19.3
3. A MongoDB instance running locally
4. *(optionally)* A Prometheus instance running locally to gather server metrics
5. Like 10 minutes you are willing to spend looking at gradle compiling kotlin

#Actual usage steps:

* Download the JAR file

You can download the plugin by clicking [here](https://github.com/ItzzInvincible/Macrocosm-2.0/releases/tag/latest)
Two jar files will appear , you want the one, titled something like `Macrocosm-<version>.jar`, and not `Macrocosm-<version>-dev.jar`.

* Setting up the MongoDB *(optional)*

You will need an empty database named `macrocosm` and a user:

in `mongosh`:
```js
use macrocosm
db.createUser({user:"<your username>", pwd:"<your password>", roles: ["dbOwner"]})
```

Remember the credentials, we will need them later.

* (Optional) Set up Prometheus to scrap Macrocosm metrics

Add this to your `prometheus.yml`:

```yml
scrape_configs:
    # ...
    - job_name: "Macrocosm"
      static_configs:
          - targets: ["localhost:3438"]
```

Don't forget to restart prometheus

```bash
sudo systemctl restart prometheus.service
```

* Getting the dependencies for Macrocosm

Macrocosm really requires only two dependency jars that it does not pull automatically: [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) and [LibsDisguises](https://github.com/libraryaddict/LibsDisguises).

* Running a server with Macrocosm

**Macrocosm requires Paper 1.19.3**. So don't use Spigot or Bukkit.
Put previously obtained `Macrocosm-<version>.jar` into the `plugins` folder, as well as `ProtocolLib` and `LibsDisguises`.

* Fill out the configs

The first run the server is probably going to error out, so you need to fill the configs.

`plugins/Macrocosm/config.yml`:
```yaml
connections:
    mongo:
        enabled: bool <set this to true to enable the server>
        username: string <your mongo username>
        password: string <your mongo password>
    discord:
        enabled: bool <set this to false if you wish to opt-out out of discord, otherwise true>
        bot-token: string <AUTH TOKEN OF THE DISCORD BOT>
        communication-channel: int <DISCORD CHANNEL ID TO BROADCAST SERVER CHAT (optional)>
        communication-webhook: string <DISCORD WEBHOOK THAT BROADCASTS THE SERVER CHAT (optional)>
        guild-id: int <YOUR DISCORD SERVER ID>
game:
    sandbox: bool <WHETHER THE GAME IS IN SANDBOX MODE> 
```

After that you can launch the server. Do note, that not using the discord features may give you tons of errors in the console, it was not tested yet.
