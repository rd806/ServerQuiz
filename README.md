## Introduction

This plugin is based on `org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT`, branching out from my previous project [zhisuan11core](https://github.com/rd806/zhisuan11core).

Generally, you can regard it as a simple **Database Management System**, which manage a Q&A system in a Minecraft server. 

> It also helps me prepare in advance for my database system course assignments.

Now, it uses [AnvilGUI](https://github.com/WesJD/AnvilGUI) to provide blank quiz system.

## Configuration

The Q&A system has two ways to storage data: YAML, MYSQL. 

The config file has been updated! You can easily set some configurations now.

> Since plugin version 1.1.0, fill-in-the-blanks quiz has been supported!

If you choose `YAML` type, please set your quiz at `Quiz.yml` in the format below:

```yaml
Choice:
  - Question: "Which of the following is not a valid IP address?"
    Options:
      A: "172.28.45.56"
      B: "192.168.0.1"
      C: "240e:360:6f43:500:d14d:dbd0:97:b3b3"
      D: "127.256.3.34"
    Answer: D
    Reward: DIAMOND

Blank:
  - Question: "1+1=?"
    Answer: "2"
    Reward: DIAMOND
```

Or if you prefer `MYSQL`, please create a new database named `serverquiz` and set your basic config:

```yaml
config:
  storage: MYSQL
  host: { your host }
  port: { your port }
  name: { your name }
  password: { your password }
  database: serverquiz
```

Other config options is shown below:

```yml
# The time between each quiz, the unit is seconds
interval: 900

# Broadcast messages when a new quiz begin
broadcast:
  # The text must be written in a single line
  text: "§e============\n§6§lNew Quiz begins! \n【Click here】 to join§r§e\n============"
  hover: "§7Click to join Server Quiz!"

# Messages sent when player answer a quiz
messages:
  # A player has already given the correct answer
  hasWinner: "The quiz has already been solved!"
  # You have answered the quiz
  hasAnswered: "You have already answered this quiz!"
  # Your answer is right
  correct: "You have solved the quiz!"
  # Your answer is wrong
  wrong: "Your answer is wrong!"
  # Update player's score
  score: "Your score board has been updated!"

# The GUI text
gui:
  title: "Quiz Menu"
  question: "Question"
  reward: "Reward"
  # Available for choice quiz
  option: "Option "
  # Available for blank quiz
  answerSheet: "Answer sheet"
  answerTitle: "Type your Answer"
  # Scoreboard settings
  scoreBoard:
    title: "§rYour quiz score board"
    # ServerQuiz has some built-in variables, which can be used here:
    # - %serverquiz_name% : the player's name
    # - %serverquiz_correct% : the number of questions correctly answered by the player
    # - %serverquiz_total% : the total number of questions answered by the player
    # - %serverquiz_accuracy% : the accuracy of the player answering questions
    lore:
      - ""
      - "§ePlayer: §f %serverquiz_name%"
      - "§eCorrect: §f %serverquiz_correct%"
      - "§eTotal: §f %serverquiz_total%"
      - "§eAccuracy: §f %serverquiz_accuracy%"
```

## Command

| Command            | Function                | Permission          |
|--------------------|-------------------------|---------------------|
| `/quiz open`       | open the answer menu    | `serverquiz.open`   |
| `/quiz send <num>` | send a quiz             | `serverquiz.edit`   |
| `/quiz show`       | show the config details | `serverquiz.show`   |
| `/quiz edit`       | edit the config         | `serverquiz.edit`   |
| `/quiz reload`     | reload the config       | `serverquiz.edit`   |

> Since version 1.1.1, you can change quiz in the game using command `/quiz edit choice` or `/quiz edit blank`. 
> 
> However, this function is still in development. You'd better do it through Database Manager.