## Introduction

This plugin is based on `org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT`, independent from my previous project [zhisuan11core](https://github.com/rd806/zhisuan11core).

Generally, you can regard it as a simple **Database Management System**, which manage a Q&A system in a Minecraft server. 

> It also help me prepare in advance for my database system course assignments.


## Configuration

The Q&A system has two ways to storage data: YAML, MYSQL.

If you choose `YAML` type, please set your quiz at `Quiz.yml` in the format below:

```yaml
List:
  - Question: "Which of the following is not a valid IP address?"
    Options:
      A: "172.28.45.56"
      B: "192.168.0.1"
      C: "240e:360:6f43:500:d14d:dbd0:97:b3b3"
      D: "127.256.3.34"
    Answer: D
    Reward: DIAMOND
```

Or if you prefer `MYSQL`, please create a new database named `quizplugin` and set your basic config:

```yaml
config:
  storage: MYSQL
  host: { your host }
  port: { your port }
  name: { your name }
  password: { your password }
  database: quizplugin
```

## Command

| Command            | Function                | Permission          |
|--------------------|-------------------------|---------------------|
| `/quiz open`       | open the answer menu    | `quizplugin.open`   |
| `/quiz send <num>` | send a quiz             | `quizplugin.send`   |
| `/quiz show`       | show the config details | `quizplugin.show`   |
| `/quiz edit`       | edit the config         | `quizplugin.edit`   |
| `/quiz reload`     | reload the config       | `quizplugin.reload` |