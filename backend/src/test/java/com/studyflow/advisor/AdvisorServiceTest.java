package com.studyflow.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyflow.advisor.AdvisorContext.DayLoad;
import com.studyflow.advisor.AdvisorContext.EventProgress;
import com.studyflow.event.EventType;
import com.studyflow.plan.PlanView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvisorServiceTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

  private final RuleBasedAdvisor rules = new RuleBasedAdvisor();

  @Test
  void usesTheAiNoteWhenTheAiAnswers() {
    AdvisorNote aiNote = new AdvisorNote("Semana tranquila.", List.of(), AdvisorNote.SOURCE_AI);
    AdvisorService service = new AdvisorService(rules, context -> aiNote);

    assertThat(service.advise(context(List.of()))).isEqualTo(aiNote);
    assertThat(service.aiEnabled()).isTrue();
  }

  @Test
  void fallsBackToTheRulesWhenTheAiFails() {
    AdvisorService service =
        new AdvisorService(
            rules,
            context -> {
              throw new IllegalStateException("provider is down");
            });

    AdvisorNote note = service.advise(context(List.of()));

    assertThat(note.source()).isEqualTo(AdvisorNote.SOURCE_RULES);
    assertThat(note.summary()).contains("1h40").contains("2 blocos");
  }

  @Test
  void usesTheRulesWhenNoAiIsConfigured() {
    AdvisorService service = new AdvisorService(rules, null);

    assertThat(service.advise(context(List.of())).source()).isEqualTo(AdvisorNote.SOURCE_RULES);
    assertThat(service.aiEnabled()).isFalse();
  }

  @Test
  void warnsWhenTheEffortDoesNotFit() {
    PlanView.Warning warning =
        new PlanView.Warning(
            7, "Prova de Banco de Dados", "Banco de Dados", EventType.EXAM, MONDAY.plusDays(3), 90);

    AdvisorNote note = rules.advise(context(List.of(warning)));

    assertThat(note.summary()).contains("faltam 1h30");
    assertThat(note.tips().get(0)).contains("Prova de Banco de Dados").contains("26/11");
  }

  @Test
  void explainsWhatChangedOnAReplan() {
    PlanView.Changes changes = new PlanView.Changes(3, 2, 4, List.of());
    AdvisorContext base = context(List.of());
    AdvisorContext replan =
        new AdvisorContext(
            base.weekStart(),
            base.today(),
            true,
            base.stats(),
            base.warnings(),
            changes,
            base.days(),
            base.events());

    assertThat(rules.advise(replan).summary()).startsWith("Refiz o plano: 3 blocos novos");
  }

  private static AdvisorContext context(List<PlanView.Warning> warnings) {
    int missing = warnings.stream().mapToInt(PlanView.Warning::missingMinutes).sum();
    return new AdvisorContext(
        MONDAY,
        MONDAY,
        false,
        new PlanView.Stats(180, 100, 0, missing),
        warnings,
        null,
        List.of(new DayLoad(MONDAY, 100, 2)),
        List.of(
            new EventProgress(
                "Prova de Banco de Dados",
                "Banco de Dados",
                "EXAM",
                MONDAY.plusDays(3),
                190,
                0,
                100)));
  }
}
