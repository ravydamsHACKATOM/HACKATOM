package com.studyflow.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyflow.advisor.AdvisorContext.DayLoad;
import com.studyflow.advisor.AdvisorContext.EventProgress;
import com.studyflow.event.EventType;
import com.studyflow.plan.PlanView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedAdvisorTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

  private final RuleBasedAdvisor advisor = new RuleBasedAdvisor();

  @Test
  void summarizesTheWeek() {
    AdvisorNote note = advisor.advise(context(List.of(), false, null));

    assertThat(note.summary()).contains("1h40").contains("2 blocos");
  }

  @Test
  void warnsWhenTheEffortDoesNotFit() {
    PlanView.Warning warning =
        new PlanView.Warning(
            7, "Prova de Banco de Dados", "Banco de Dados", EventType.EXAM, MONDAY.plusDays(3), 90);

    AdvisorNote note = advisor.advise(context(List.of(warning), false, null));

    assertThat(note.summary()).contains("faltam 1h30");
    assertThat(note.tips().get(0)).contains("Prova de Banco de Dados").contains("26/11");
  }

  @Test
  void explainsWhatChangedOnAReplan() {
    PlanView.Changes changes = new PlanView.Changes(3, 2, 4, List.of());

    AdvisorNote note = advisor.advise(context(List.of(), true, changes));

    assertThat(note.summary()).startsWith("Refiz o plano: 3 blocos novos");
  }

  @Test
  void pointsOutTheNextExam() {
    AdvisorNote note = advisor.advise(context(List.of(), false, null));

    assertThat(note.tips()).anyMatch(tip -> tip.contains("Prova de Banco de Dados"));
  }

  @Test
  void neverGivesMoreThanFourTips() {
    List<PlanView.Warning> warnings =
        List.of(1, 2, 3, 4, 5, 6).stream()
            .map(
                id ->
                    new PlanView.Warning(
                        id, "Entrega " + id, "Redes", EventType.ASSIGNMENT, MONDAY.plusDays(2), 60))
            .toList();

    assertThat(advisor.advise(context(warnings, false, null)).tips()).hasSize(4);
  }

  private static AdvisorContext context(
      List<PlanView.Warning> warnings, boolean replan, PlanView.Changes changes) {
    int missing = warnings.stream().mapToInt(PlanView.Warning::missingMinutes).sum();
    return new AdvisorContext(
        MONDAY,
        MONDAY,
        replan,
        new PlanView.Stats(180, 100, 0, missing),
        warnings,
        changes,
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
