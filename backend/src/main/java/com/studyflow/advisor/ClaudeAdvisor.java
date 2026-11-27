package com.studyflow.advisor;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;

/**
 * Asks Claude to explain the plan in plain language.
 *
 * <p>The model never decides the schedule: it receives the facts computed by the planner and only
 * turns them into a short explanation and practical tips. That keeps every block inside the
 * student's real availability no matter what the model says.
 */
public class ClaudeAdvisor implements PlanAdvisor {

  private static final String SYSTEM_PROMPT =
      """
      Você é o orientador de estudos do StudyFlow, um planejador para estudantes de curso técnico.
      Você recebe, em JSON, os fatos de um plano semanal que já foi calculado por um algoritmo:
      horas disponíveis, blocos por dia, provas e entregas, o que mudou em relação ao plano
      anterior e o que não coube antes dos prazos.

      Explique o plano para o estudante em português do Brasil, com tom calmo, direto e humano.
      - "summary": de uma a três frases dizendo como ficou a semana. Se "replan" for verdadeiro,
        comece explicando o que mudou. Se houver "warnings", diga com clareza que o volume não
        coube e quanto falta.
      - "tips": de uma a quatro dicas curtas e práticas, cada uma ligada a um fato do JSON, como
        o dia mais cheio, a prova mais próxima, o que cortar ou antecipar quando falta tempo, ou
        uma revisão curta na véspera.

      Use apenas os fatos recebidos. Não invente horários, datas, disciplinas ou conteúdos de
      prova, e não prometa resultados. Escreva durações como "1h30" ou "50 min" e datas como
      "30/11". Não use markdown nem emojis.
      """;

  private final AnthropicClient client;
  private final String model;
  private final ObjectMapper json;

  public ClaudeAdvisor(AiProperties properties, ObjectMapper json) {
    this.client =
        AnthropicOkHttpClient.builder()
            .apiKey(properties.apiKey())
            .baseUrl(properties.baseUrl())
            .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
            .maxRetries(1)
            .build();
    this.model = properties.model();
    this.json = json;
  }

  @Override
  public AdvisorNote advise(AdvisorContext context) {
    StructuredMessageCreateParams<Reply> params =
        MessageCreateParams.builder()
            .model(model)
            .maxTokens(2000L)
            .system(SYSTEM_PROMPT)
            .outputConfig(Reply.class)
            .addUserMessage(facts(context))
            .build();

    StructuredMessage<Reply> message = client.messages().create(params);
    Reply reply =
        message.content().stream()
            .flatMap(block -> block.text().stream())
            .map(text -> text.text())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("The model returned no text"));
    if (reply.summary == null || reply.summary.isBlank()) {
      throw new IllegalStateException("The model returned an empty summary");
    }
    List<String> tips = reply.tips == null ? List.of() : reply.tips.stream().limit(4).toList();
    return new AdvisorNote(reply.summary.strip(), tips, AdvisorNote.SOURCE_AI);
  }

  private String facts(AdvisorContext context) {
    try {
      return json.writeValueAsString(context);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not serialize the plan facts", e);
    }
  }

  /** Shape the model must answer in; the SDK derives the JSON schema from it. */
  public static class Reply {

    @JsonPropertyDescription("One to three sentences describing how the week looks.")
    public String summary;

    @JsonPropertyDescription("One to four short, practical tips tied to facts of the plan.")
    public List<String> tips;
  }
}
