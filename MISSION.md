120 Days of Backend Engineering

Who I am

I am a software engineer with ~6 years of commercial development experience.

My background is frontend-heavy fullstack development. I have worked with Vue.js, Laravel, APIs, databases, integrations, production systems, monitoring, business logic, and real product constraints.

I am not starting software engineering from zero.

I already know how to ship software, navigate existing codebases, debug production problems, communicate engineering decisions, work with business requirements, and take ownership of features.

What I am changing is my specialization.

I want backend engineering to become a first-class part of my professional identity rather than something adjacent to my frontend work.

⸻

Why I am doing this

Frontend no longer feels like the direction in which I want to grow.

I don’t want the next several years of my career to primarily consist of becoming better at UI frameworks, browser APIs, component architecture, state management, or another geneon of frontend tooling.

I am much more interested in what happens behind the API:

* how data is stored;
* how concurrent operations interact;
* how transactions work;
* how systems remain consistent;
* how services communicate;
* what happens when one of them fails;
* how systems behave under load;
* how distributed systems are designed;
* how architecture evolves;
* how production incidents are investigated;
* how engineering trade-offs are made.

I want to understand the system, not only the interface to it.

This is not studying backend because I am bored.

This is an intentional attempt to change the trajectory of my engineering career.

⸻

The goal

For the next 120 days, I will deliberately move from being a fntend-heavy engineer toward becoming a backend-heavy software engineer.

My primary stack will be:

* Java
* Spring Boot
* PostgreSQL
* Redis
* Kafka
* Docker

But learning technologies is not the actual goal.

The actual goal is to become good at:

* backend engineering;
* databases;
* concurrency;
* distributed systems;
* system design;
* software architecture;
* reliability;
* observability;
* production engineering.

Java and Spring are the tools I will use to get there.

⸻

Career target

At the end of this challenge, I want to be capable of interviewing for Middle Java / Backend Software Engineer positions, particularly in banks and fintech companies in Uzbekistan.

This has two separate requirements:

1. I need to become capable of doing the job.
2. I need to become capable of pasng the interview.

These are related, but they are not the same skill.

I will deliberately train both.

I should expect interviews to contain a relatively predictable pool of questions around Java, JVM, Spring, SQL, transactions, concurrency, messaging, distributed systems and system design.

Knowing how something works is not enough if I cannot explain it clearly under interview conditions.

Therefore interview preparation is not something that begins on Day 119.

It is part of the 120-day program.

⸻

Interview reality

Switching specialization after years of frontend-heavy work creates an awkward market position.

I am not a junior software engineer.

But I also cannot honestly claim years of commercial Java experience that I do not have.

Some companies will reject me because of that.

That is expected.

I  not need every company to accept this transition.

I need one company to see enough engineering maturity and backend competence to make the bet.

At the same time, I need to become very good at presenting the experience I actually have.

I should not describe myself like this:

“I’m basically a frontend developer, but recently I started learning some Java and I’d like to try backend.”

That framing throws away years of engineering experience before the interview has even stanstead, I need to be able to explain my career accurately from a software-engineering perspective:

“My recent roles have been frontend-heavy, but my work has regularly crossed backend and system boundaries: APIs, data contracts, business logic, integrations, observability, performance, production incidents and end-to-end feature ownership. I am now deliberately shifting my specialization toward backend engineering and using Java/Spring as my primary backend stack.”

The exact wording will evolve as my backend experience grows.

The important principle is:

Do not minimize relevant experience simply because the job title or primary stack was frontend.

⸻

I need backend stories

Interviewers do not only ask definitions.

They ask:

Tell me about a difficult production problem.

Tell me about a performance problem you investigated.

Tell me about an architectural decision you made.

Tell me about a database problem.

Tell me about a failure.

Tell me about a system you designed.

Why did you choose pproach?

I need good answers.

During these 120 days I will therefore build a collection of backend stories.

Some will come from my existing commercial experience.

Some will come from MiniBank and deliberate engineering experiments.

For each important topic, I should eventually be capable of explaining:

PROBLEM
↓
CONTEXT
↓
OPTIONS
↓
DECISION
↓
IMPLEMENTATION
↓
FAILURE MODES
↓
RESULT
↓
WHAT I WOULD CHANGE

I should be able to talk about backend engineering as something I have personally touched, debugged, built and reasoned about.

Because by the end of these 120 days, that should actually be true.

⸻

Resume strategy

My CV will need to evolve together with my skills.

I should identify bparts of my previous work and describe them clearly:

* API contracts;
* backend integrations;
* data flows;
* performance work;
* observability;
* production incidents;
* business logic;
* reliability;
* architecture;
* end-to-end ownership.

If a feature involved frontend + backend + infrastructure + monitoring, describing it only as:

“Implemented Vue components”

is actively underselling my work.

The backend-relevant engineering should be visible.

However, there is a line I should not cross.

I can reframe real experience.

I can emphasize relevant parts of real projects.

I can describe the system around work I genuinely participated in and clearly distinguish what I personally owned.

I can build substantial Java experience through this project.

I should not invent production incidents, responsibilities, systems, employers, or years of commercial Java work that never happened.

The objectiv not to become better at lying.

The objective is to make the gap between:

“how experienced I sound”

and

“what I can actually do”

as small as possible.

⸻

The interview version of me

By the end of the challenge I need a strong answer to:

Tell me about yourself.

It should communicate three things.

1. I am already an experienced engineer

I have years of commercial software development behind me.

I understand production software and product development.

2. My experience is broader than UI

I have worked across system boundaries and have experience with APIs, business logic, data, integrations, performance, observability and end-to-end ownership.

3. Backend is now an intentional specialization

I am not randomly applying for Java positions.

I have deliberately invested months into Java, Spring, databases, concurrency, distributed systems and system design, and I have built systems specifically to develop those skills.

I should sound like someone making an engineering spec switch, not someone entering programming for the first time.

⸻

The project

The main project for these 120 days is MiniBank.

It will start deliberately simple.

Client
  |
  v
Spring Boot
  |
  v
PostgreSQL

The first version will barely know how to transfer money.

Then I will deliberately discover and introduce problems.

Account A: 1,000,000 UZS
Account B:   500,000 UZS
A -> B: 200,000 UZS

What happens if the application crashes after debiting A but before crediting B?

What happens if two transfers spend the same balance simultaneously?

What happens if the client sends the same request twice?

What happens if the database becomes unavailable?

What happens when multiple application instances process requests?

What happens when another service needs to know that the transfer happened?

What happens if Kafka is unavailable?

What happens if a consumer receives the same event twice?

What happens at 10 requests per second?

At 1,000?

At 10,000?

Every problem should introduce the next engineeringoncept naturally.

⸻

Learning philosophy

The sequence should generally be:

BUILD
  ↓
BREAK
  ↓
UNDERSTAND WHY IT BROKE
  ↓
LEARN
  ↓
FIX
  ↓
TEST
  ↓
DOCUMENT

Not:

WATCH COURSE
  ↓
WATCH ANOTHER COURSE
  ↓
READ ARTICLE
  ↓
FEEL SMART
  ↓
FORGET EVERYTHING

I should encounter a reason to learn a technology before introducing it whenever possible.

I don’t need Kafka because “backend developers should know Kafka.”

I need Kafka when my system develops a problem for which asynchronous messaging is a reasonable solution.

And then I need to understand what new problems Kafka itself creates.

⸻

Daily rule

Every day should leave behind at least one artifact.

An artifact can be:

* working code;
* a test;
* a reproduced bug;
* a benchmark;
* an ADR;
* a system-design diagram;
* a database experiment;
* an incident investigation;
* documentation;
* an interview answer;
* a small prototype;
* a written summary of a course lesson or book chapter;
* any backend task completed at work.

A backend task at work counts as an artifact on its own. Real production work with real constraints, real data and real consequences is a different kind of practice than any learning project.

When possible, I should leave a short note about it (problem, decision, trade-offs, what went wrong), without copying confidential code or data. These notes are the raw material for my backend stories.

A course lesson or book chapter counts as an artifact only when it produces written notes with my own conclusions: what I learned, what I missed, which trade-offs matter, and how it relates to MiniBank.

The lesson should be part of the one structured course or book I am currently following. This is strongly preferred.

A random article or a random video does not count, even if I take notes on it. Otherwise "I read something interesting" slowly replaces actual progress.

Passively watching or reading without written conclusions is not an artifact.

⸻

No zero days

The default target is approximately 60–120 minutes per day.

Some days will be longer.

Some days will suck.

If I genuinely don’t have enough time, the minimum acceptable session can be smaller.

But the important thing is continuity.

I am not trying to have one insane 8-hour Saturday.

I am trying to accumulate:

120 days × deliberate practice.

Consistency beats occasional heroics.

⸻

Every 7th day — checkpoint

Every seventh day should contain some form of review.

I should be able to explain the week’s topics without Google, ChatGPT, documentation, or notes.

Questions should resemble real interview questions.

If I can use something but cannot explain it, I don’t understand it well enough yet.

If I can explain something but cannot build it, I don’t understand it well enough either.

Both mnes

I will take a backend knowledge baseline on:

* Day 1
* Day 60
* Day 120

The purpose is not to produce a nice score.

The purpose is to expose blind spots and measure actual progress.

A low Day 1 score is useful.

It tells me where I am.

⸻

Interview phase

Interview preparation begins early.

Every week I should practice explaining technical concepts aloud.

Later, this becomes structured mock interviewing.

Around Day 60–90 I should be capable of answering increasingly realistic Java/backend interview questions.

Around Day 90 I should start applying.

I should expect to fail some interviews.

That is not a failure of the challenge.

An interview that exposes five weaknesses has given me five concrete things to improve.

Interview
   ↓
Got destroyed on a topic
   ↓
Study it
   ↓
Build something with it
   ↓
Explain it
   ↓
Try again

The objective is not to pass eve
I need one good offer.

⸻

Things I will NOT optimize for

I will not try to collect technologies.

I will not add infrastructure simply because it looks impressive.

I will not build microservices when a monolith solves the problem.

I will not memorize system-design diagrams without understanding them.

I will not spend three days making MiniBank’s package structure beautiful.

I will not confuse watching educational content with engineering practice.

I will not restart the roadmap every two weeks because I found a better roadmap on the internet.

And I will not allow “I haven’t used this commercially for three years” to become an excuse for weak technical knowledge.

If something is learnable, I will learn it.

If something is buildable, I will build it.

If something is commonly asked in intervil prepare for it.

⸻

What I already bring

I am not throwing away my previous career.

My existing engineering experience still matters.

I already understand things that cannot be learned from a Java syntax course:

* working with real products;
* navigating large existing codebases;
* debugging;
* production constraints;
* communication with other engineers;
* business requirements;
* ownership;
* trade-offs;
* shipping software used by real people.

The goal of these 120 days is to put a much stronger backend foundation underneath that experience.

⸻

Why this matters to me

I don’t want to continue moving deeper into a specialization simply because it is the specialization I already have.

Six years of experience is not a reason to spend another six years going in the same direction.

If I find databases, distributed systems, architecture, reliability and b engineering more interesting than frontend work, the rational thing is to test that direction seriously.

These 120 days are that test.

Not:

“Do I enjoy watching videos about backend?”

But:

“Do I enjoy actually doing backend engineering when it becomes difficult?”

By Day 120 I should have a much better answer.

And if the answer is yes, I want to make the switch for real.

⸻

Day 120

Day 1

I am an experienced frontend-heavy engineer learning Java backend.

Day 120

I am an experienced software engineer who has deliberately shifted toward backend engineering. I can design, implement, test, debug and reason about backend systems, explain my decisions under interview pressure, and I am ready to get paid to do it.

The goal is not to look like a backend engineer.

The goal is to make the distinction increasingly meaningless.

⸻

Start

Do not optimize Day 1.

Do not think about Day 120.

Build the first broken version of MiniBank.

Then make it better tomorrow 120.
