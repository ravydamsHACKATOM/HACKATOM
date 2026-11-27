# StudyFlow — Backend

Spring Boot REST API for the StudyFlow study planner. This starter implements the backend MVP without AI integration and without imposing a frontend design. The Figma screens will be used later to build the frontend faithfully.

## Requirements

- Java 21.0.7 (the Maven wrapper uses the project-local JDK in `.jdks` when present)
- Maven 3.9+ (or use the Maven wrapper if you add one)
- Internet access on the first build to download dependencies

## Run

From the project root:

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. The SQLite database is created at `./data/studyflow.db`.

To build and test:

```bash
mvn clean test
mvn clean package
```

## Main endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET, POST | `/api/disciplines` | List/create disciplines |
| GET, PUT, DELETE | `/api/disciplines/{id}` | Read/update/delete a discipline |
| GET, POST | `/api/events` | List/create exams and assignments |
| GET, PUT, DELETE | `/api/events/{id}` | Read/update/delete an event |
| GET, POST | `/api/availabilities` | List/create weekly availability |
| PUT, DELETE | `/api/availabilities/{id}` | Update/delete availability |
| POST | `/api/schedule/generate?weekStart=2026-11-23` | Generate the week's study blocks |
| GET | `/api/blocks?from=2026-11-23&to=2026-11-29` | List blocks in a date range |
| GET | `/api/blocks/today?date=2026-11-26` | List a day's study blocks |
| PATCH | `/api/blocks/{id}/completion` | Mark a block completed/incomplete |
| GET | `/api/health` | Check API status |

## Example requests

Create a discipline:

```json
{
  "name": "Programming",
  "color": "#7C6CF2",
  "description": "Java and Spring Boot"
}
```

Create an event (replace `disciplineId` with an existing ID):

```json
{
  "title": "Spring Boot assessment",
  "eventType": "EXAM",
  "dueDate": "2026-11-29",
  "estimatedHours": 3,
  "disciplineId": 1,
  "description": "Review REST APIs and JDBC"
}
```

Create availability (1 = Monday, 7 = Sunday):

```json
{
  "dayOfWeek": 1,
  "startTime": "18:00",
  "endTime": "21:00"
}
```

Mark a block as completed:

```json
{
  "completed": true
}
```

## Scheduling rules in this initial version

- The schedule generator uses the registered recurring availability for each weekday.
- It creates 50-minute study sessions with a 10-minute gap between sessions.
- Events are considered in due-date order.
- Sessions are placed on or before the event's due date, within the selected Monday–Sunday week.
- When generating again, only pending blocks in that week are replaced; completed blocks are preserved.
- If there is not enough availability, the API creates as many sessions as fit. The remaining effort is not yet shown as a separate warning; this is a known MVP improvement.

## Notes

- This is the backend foundation. The frontend will be implemented after reviewing the actual Figma/prototype screenshots.
- No AI provider or AI feature is connected.
- CORS currently allows common local frontend origins (`localhost:5500` and `localhost:3000`). Adjust `CorsConfig` if your frontend uses another port.
- SQLite database files are local and ignored by Git.
