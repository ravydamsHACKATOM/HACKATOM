package com.studyflow.advisor;

import com.studyflow.advisor.AdvisorContext.DayLoad;
import com.studyflow.advisor.AdvisorContext.EventProgress;
import com.studyflow.plan.PlanView;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Explains the plan with fixed rules: how the week looks, what changed and what to watch.
 * User-facing text is in Portuguese.
 */
@Component
public class RuleBasedAdvisor implements PlanAdvisor {

  private static final Locale PT_BR = Locale.of("pt", "BR");
  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
  private static final int HEAVY_DAY_MINUTES = 180;
  private static final int MAX_TIPS = 4;

  @Override
  public AdvisorNote advise(AdvisorContext context) {
    return new AdvisorNote(summary(context), tips(context));
  }

  private static String summary(AdvisorContext context) {
    PlanView.Stats stats = context.stats();
    int blocks = context.days().stream().mapToInt(DayLoad::blocks).sum();
    if (blocks == 0 && context.warnings().isEmpty()) {
      return "Ainda não há o que planejar nesta semana. Cadastre uma prova ou entrega e os"
          + " horários em que você pode estudar.";
    }

    StringBuilder text = new StringBuilder();
    PlanView.Changes changes = context.changes();
    if (context.replan() && changes != null && !changes.isEmpty()) {
      text.append("Refiz o plano: ")
          .append(plural(changes.added(), "bloco novo", "blocos novos"))
          .append(" e ")
          .append(plural(changes.removed(), "removido", "removidos"))
          .append(". ");
    }
    text.append("São ")
        .append(Durations.format(stats.plannedMinutes()))
        .append(" de estudo em ")
        .append(plural(blocks, "bloco", "blocos"))
        .append(", sempre dentro dos seus horários livres.");
    if (!context.warnings().isEmpty()) {
      text.append(" Atenção: faltam ")
          .append(Durations.format(stats.missingMinutes()))
          .append(" que não couberam antes dos prazos.");
    }
    return text.toString();
  }

  private static List<String> tips(AdvisorContext context) {
    List<String> tips = new ArrayList<>();

    for (PlanView.Warning warning : context.warnings()) {
      tips.add(
          "\"%s\" (%s) ainda precisa de %s antes de %s. Abra mais um horário ou reveja a estimativa."
              .formatted(
                  warning.title(),
                  warning.disciplineName(),
                  Durations.format(warning.missingMinutes()),
                  warning.dueDate().format(DAY_MONTH)));
    }

    context.days().stream()
        .max(Comparator.comparingInt(DayLoad::plannedMinutes))
        .filter(day -> day.plannedMinutes() >= HEAVY_DAY_MINUTES)
        .ifPresent(
            day ->
                tips.add(
                    "%s é o dia mais cheio (%s). Se cansar, adie um bloco e eu redistribuo o resto."
                        .formatted(
                            capitalize(
                                day.date().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR)),
                            Durations.format(day.plannedMinutes()))));

    context.events().stream()
        .filter(event -> "EXAM".equals(event.type()))
        .filter(event -> !event.dueDate().isBefore(context.today()))
        .min(Comparator.comparing(EventProgress::dueDate))
        .ifPresent(
            exam -> {
              long days = ChronoUnit.DAYS.between(context.today(), exam.dueDate());
              String when =
                  days == 0 ? "é hoje" : days == 1 ? "é amanhã" : "é em " + days + " dias";
              tips.add(
                  "A prova \"%s\" %s. A última sessão antes dela é uma revisão leve, sem conteúdo novo."
                      .formatted(exam.title(), when));
            });

    int free = context.stats().availableMinutes() - context.stats().plannedMinutes();
    if (context.warnings().isEmpty() && free >= 120) {
      tips.add(
          "Sobram cerca de %s livres na semana. Dá para adiantar uma entrega ou guardar como folga."
              .formatted(Durations.format(free)));
    }
    return tips.stream().limit(MAX_TIPS).toList();
  }

  private static String plural(int count, String one, String many) {
    return count + " " + (count == 1 ? one : many);
  }

  private static String capitalize(String text) {
    return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
  }
}
