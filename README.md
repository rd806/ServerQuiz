# Server Quiz

This plugin is based on `org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT`, branching out from my previous project [zhisuan11core](https://github.com/rd806/zhisuan11core).

Generally, you can regard it as a simple **Database Management System**, which manage a Q&A system in a Minecraft server. 

> It also helps me prepare in advance for my database system course assignments.

Now, it uses [AnvilGUI](https://github.com/WesJD/AnvilGUI) to provide blank quiz system.

## Configuration

The Q&A system has two ways to storage data: YAML, MYSQL.

> Since plugin version 1.1.0, fill-in-the-blanks quiz has been supported!

### YAML

If you choose `YAML` type, quizzes are stored as one file per bank under `plugins/ServerQuiz/quiz/`.
The file names come from `config.table` in `config.yml`, for example `choice` loads `quiz/choice.yml`.

Each file declares its type in the header and lists its items under `entry`. The item fields use the same
names as the MYSQL columns (`question`, `option_a` ~ `option_d`, `answer`, `score`, `reward`).

`quiz/choice.yml`:

```yaml
type: choice
entry:
  - question: "Which of the following is not a valid IP address?"
    option_a: "172.28.45.56"
    option_b: "192.168.0.1"
    option_c: "240e:360:6f43:500:d14d:dbd0:97:b3b3"
    option_d: "127.256.3.34"
    answer: D
    score: 10
    reward: DIAMOND
```

`quiz/blank.yml`:

```yaml
type: blank
entry:
  - question: "1+1=?"
    answer: "2"
    score: 10
    reward: DIAMOND
```

> Upgrading from an older version? The plugin automatically splits the old `Quiz.yml` into `quiz/choice.yml` and `quiz/blank.yml`, then renames the original to `Quiz.yml.migrated`.
> Files using the previous `Choice:` / `Blank:` sections are still read and are rewritten in the new format the next time the bank is saved.

For a multiple-choice question, write all correct labels separated by commas, for example `answer: "A,C"`. The order of the labels does not matter, and options are still shuffled randomly on the answer screen.

### MYSQL

If you prefer `MYSQL`, please create a new database named `serverquiz` and set your basic config in advance:

```yaml
config:
  storage: MYSQL
  host: { your host }
  port: { your port }
  name: { your name }
  password: { your password }
  database: serverquiz
  # Any number of quiz tables in the same database
  table:
    - choice
    - blank
    - history
```

You can load any number of quiz banks. With `YAML`, each entry in `table` is a file under `quiz/`; with `MYSQL`, each entry is a table in the same database. Banks are read in the listed order and numbered sequentially. For MYSQL, a table is recognized by its columns:

- A table with `option_a` ~ `option_d` is loaded as a **choice** bank.
- A table without those columns is loaded as a **blank** bank.
- Every table needs at least `id`, `question`, `answer`, `score` and `reward`; missing `score`/`reward` columns are added automatically.
- Tables that do not exist or have an incompatible schema are skipped with a warning.

Adding, removing or editing tables takes effect with `/quiz reload`, no server restart needed.

The configuration file has been updated! You can easily set some configurations now. Other config options is shown below:

```yml
###########################################
##             Update checker            ##
###########################################

updateChecker:
  # Option: modrinth/github
  source: modrinth
  enable: true

##############################################
##            Broadcast messages            ##
##############################################

broadcast:
  # The text must be written in a single line
  text: "§e============\n§6§lNew Quiz begins! \n【Click here】 to join§r§e\n============"
  hover: "§7Click to join Server Quiz!"
  # When to broadcast the next quiz. Must be a quoted cron expression.
  # 5 fields: minute hour day-of-month month day-of-week
  # 6 fields: second minute hour day-of-month month day-of-week
  # Day of week: 0 or 7 = Sunday, 1-6 = Monday-Saturday.
  # Examples:
  #   "*/15 * * * *"   every 15 minutes
  #   "0 */2 * * *"    every 2 hours at minute 0
  #   "0 0 20 * * *"   every day at 20:00:00
  # If the expression is missing or invalid, the plugin falls back to 900 seconds.
  interval: "*/15 * * * *"

##############################################
##             Answer messages              ##
##############################################

messages:
  # A player has already given the correct answer
  hasWinner: "The quiz has already been solved!"
  # You have answered the quiz
  hasAnswered: "You have already answered this quiz!"
  # Your answer is right
  correct: "You have solved the quiz!"
  # Your answer is wrong
  wrong: "Your answer is wrong!"
  # Send Reward
  sendReward: "Reward has been sent to you: "
  # Update player's score
  score: "Your score board has been updated!"

##############################################
##              The GUI text                ##
##############################################

gui:
  title: "Quiz Menu"
  question: "Question"
  reward: "Reward"
  # Available for choice quiz
  choice:
    option: "Option "
    # Whether to arrange options randomly
    random: true
  # Available for blank quiz
  blank:
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

You can manage quiz entries in game:

```text
# Choice bank
/quiz edit entry <table> insert <question> <optionA> <optionB> <optionC> <optionD> <answer> <score> <reward>
/quiz edit entry <table> alter <id> <question> <optionA> <optionB> <optionC> <optionD> <answer> <score> <reward>

# Blank bank
/quiz edit entry <table> insert <question> <answer> <score> <reward>
/quiz edit entry <table> alter <id> <question> <answer> <score> <reward>
```

`<id>` is the global quiz id shown by `/quiz show info <id>`. Run `/quiz reload` after editing to apply the change.