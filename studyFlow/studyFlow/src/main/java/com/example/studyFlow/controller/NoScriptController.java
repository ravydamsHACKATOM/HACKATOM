package com.example.studyFlow.controller;

import com.example.studyFlow.dto.*;
import com.example.studyFlow.repository.StudyBlockRepository;
import com.example.studyFlow.service.StudyFlowService;
import jakarta.validation.Validator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** Server-rendered forms for users who have JavaScript disabled. */
@Controller
public class NoScriptController {
    private final StudyFlowService service;
    private final StudyBlockRepository blocks;
    private final Validator validator;

    public NoScriptController(StudyFlowService service, StudyBlockRepository blocks, Validator validator) {
        this.service = service;
        this.blocks = blocks;
        this.validator = validator;
    }

    @GetMapping(value = "/no-js", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String index(Model model) {
        String message = (String) model.asMap().get("message");
        List<SubjectResponse> subjects = service.getSubjects();
        List<EventResponse> events = service.getEvents();
        List<AvailabilityResponse> availability = service.getAvailability();
        List<BlockResponse> studyBlocks = service.getStudyBlocks();
        AvailabilityResponse existingAvailability = availability.size() == 1 ? availability.get(0) : null;
        String today = LocalDate.now().toString();
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>StudyFlow — modo sem JavaScript</title><link rel=\"stylesheet\" href=\"/css/app.css\"></head><body><header class=\"topbar\"><a class=\"brand\" href=\"/no-js\"><span class=\"brand-mark\">S</span>StudyFlow</a><a href=\"/\">Interface completa</a></header><main class=\"main\" style=\"max-width:900px;margin:28px auto;padding:0 18px\"><p class=\"eyebrow\">MODO SEM JAVASCRIPT</p><h1>Gerencie seus estudos</h1><p class=\"lead\">Esta interface usa formulários HTML e salva tudo pelo backend.</p>");
        if (message != null && !message.isBlank()) html.append("<p class=\"notice\" role=\"status\">").append(escape(message)).append("</p>");

        html.append("<section class=\"panel\"><h2>Matérias</h2>");
        for (SubjectResponse subject : subjects) {
            html.append("<form class=\"item\" method=\"post\" action=\"/no-js/subjects/").append(subject.id()).append("/update\"><div class=\"item-info\"><label>Nome<input name=\"name\" maxlength=\"100\" required value=\"").append(escape(subject.name())).append("\"></label><label>Cor<input name=\"color\" maxlength=\"20\" value=\"").append(escape(subject.color())).append("\"></label></div><div class=\"item-actions\"><button class=\"button secondary\">Salvar</button></form><form method=\"post\" action=\"/no-js/subjects/").append(subject.id()).append("/delete\"><button class=\"button danger\">Excluir matéria e seus prazos</button></form></div>");
        }
        html.append("<form method=\"post\" action=\"/no-js/subjects\" class=\"form-fields\"><h3>Nova matéria</h3><label>Nome<input name=\"name\" maxlength=\"100\" required></label><label>Cor opcional<input name=\"color\" maxlength=\"20\" placeholder=\"#2858c8\"></label><button class=\"button primary\">Cadastrar matéria</button></form></section>");

        html.append("<section class=\"panel\"><h2>Provas e entregas</h2>");
        for (EventResponse event : events) {
            html.append("<form method=\"post\" action=\"/no-js/events/").append(event.id()).append("/update\" class=\"item form-fields\"><label>Título<input name=\"title\" maxlength=\"150\" required value=\"").append(escape(event.title())).append("\"></label><label>Matéria<select name=\"subjectId\" required>").append(subjectOptions(subjects, event.subjectId())).append("</select></label><label>Tipo<select name=\"type\"><option value=\"PROVA\"").append(selected(event.type().equals("PROVA"))).append(">Prova</option><option value=\"ENTREGA\"").append(selected(event.type().equals("ENTREGA"))).append(">Entrega</option></select></label><label>Prazo<input name=\"dueDate\" type=\"date\" min=\"").append(today).append("\" required value=\"").append(event.dueDate()).append("\"></label><label>Esforço em horas<input name=\"estimatedHours\" type=\"number\" min=\"0.25\" max=\"100\" step=\"0.25\" required value=\"").append(event.estimatedHours()).append("\"></label><button class=\"button secondary\">Salvar prazo</button></form><form method=\"post\" action=\"/no-js/events/").append(event.id()).append("/delete\"><button class=\"button danger\">Excluir prazo e blocos</button></form>");
        }
        html.append("<form method=\"post\" action=\"/no-js/events\" class=\"form-fields\"><h3>Novo prazo</h3><label>Título<input name=\"title\" maxlength=\"150\" required></label><label>Matéria<select name=\"subjectId\" required>").append(subjectOptions(subjects, null)).append("</select></label><label>Tipo<select name=\"type\"><option value=\"PROVA\">Prova</option><option value=\"ENTREGA\">Entrega</option></select></label><label>Prazo<input name=\"dueDate\" type=\"date\" min=\"").append(today).append("\" required></label><label>Esforço em horas<input name=\"estimatedHours\" type=\"number\" min=\"0.25\" max=\"100\" step=\"0.25\" required></label><button class=\"button primary\">Cadastrar prazo</button></form></section>");

        html.append("<section class=\"panel\"><h2>Disponibilidade semanal</h2><p>ISO: segunda = 1, domingo = 7. Salvar substitui a semana inteira.</p><form method=\"post\" action=\"/no-js/availability\" class=\"form-fields\"><fieldset><legend>Dias</legend><div class=\"day-picker\">");
        for (int day = 1; day <= 7; day++) html.append("<label class=\"day-option\"><input type=\"checkbox\" name=\"daysOfWeek\" value=\"").append(day).append("\"").append(selected(existingAvailability != null && existingAvailability.daysOfWeek().contains(day))).append("><span>").append(dayName(day)).append("</span></label>");
        html.append("</div></fieldset><div class=\"time-fields\"><label>Início<input type=\"time\" name=\"startTime\" required value=\"").append(existingAvailability == null ? "" : existingAvailability.startTime()).append("\"></label><label>Fim<input type=\"time\" name=\"endTime\" required value=\"").append(existingAvailability == null ? "" : existingAvailability.endTime()).append("\"></label></div><button class=\"button primary\">Salvar disponibilidade</button></form>");
        for (AvailabilityResponse entry : availability) html.append("<p>").append(entry.daysOfWeek().stream().map(NoScriptController::dayName).map(NoScriptController::escape).reduce((a, b) -> a + ", " + b).orElse("")).append(" · ").append(entry.startTime()).append("–").append(entry.endTime()).append("</p>");
        html.append("<form method=\"post\" action=\"/no-js/availability/clear\"><button class=\"button danger\">Limpar disponibilidade</button></form></section>");

        html.append("<section class=\"panel\"><h2>Cronograma</h2><p>Não agenda no próprio dia do prazo; blocos concluídos permanecem ao recalcular.</p><form method=\"post\" action=\"/no-js/schedule\"><button class=\"button primary\">Gerar cronograma</button></form>");
        for (BlockResponse block : studyBlocks) html.append("<form class=\"item\" method=\"post\" action=\"/no-js/blocks/").append(block.id()).append("/completion\"><div class=\"item-info\"><strong>").append(escape(block.title())).append("</strong><small>").append(block.startAt()).append(" · ").append(block.endAt()).append("</small></div><input type=\"hidden\" name=\"completed\" value=\"").append(!block.completed()).append("\"><button class=\"button secondary\">").append(block.completed() ? "Reabrir bloco" : "Concluir bloco").append("</button></form>");
        html.append("</section><p><a href=\"/\">Voltar à interface completa</a></p></main></body></html>");
        return html.toString();
    }

    @PostMapping("/no-js/subjects")
    public String createSubject(@RequestParam String name, @RequestParam(required = false) String color, RedirectAttributes redirect) {
        return perform(redirect, () -> { validate(new SubjectRequest(name, color)); service.createSubject(new SubjectRequest(name, color)); });
    }

    @PostMapping("/no-js/subjects/{id}/update")
    public String updateSubject(@PathVariable Long id, @RequestParam String name, @RequestParam(required = false) String color, RedirectAttributes redirect) {
        return perform(redirect, () -> { validate(new SubjectRequest(name, color)); service.updateSubject(id, new SubjectRequest(name, color)); });
    }

    @PostMapping("/no-js/subjects/{id}/delete")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirect) { return perform(redirect, () -> service.deleteSubject(id)); }

    @PostMapping("/no-js/events")
    public String createEvent(@RequestParam String title, @RequestParam String type, @RequestParam Long subjectId,
                              @RequestParam LocalDate dueDate, @RequestParam Double estimatedHours, RedirectAttributes redirect) {
        EventRequest request = new EventRequest(title, type, subjectId, dueDate, estimatedHours);
        return perform(redirect, () -> { validate(request); service.createEvent(request); });
    }

    @PostMapping("/no-js/events/{id}/update")
    public String updateEvent(@PathVariable Long id, @RequestParam String title, @RequestParam String type, @RequestParam Long subjectId,
                              @RequestParam LocalDate dueDate, @RequestParam Double estimatedHours, RedirectAttributes redirect) {
        EventRequest request = new EventRequest(title, type, subjectId, dueDate, estimatedHours);
        return perform(redirect, () -> { validate(request); service.updateEvent(id, request); });
    }

    @PostMapping("/no-js/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirect) { return perform(redirect, () -> service.deleteEvent(id)); }

    @PostMapping("/no-js/availability")
    public String saveAvailability(@RequestParam List<Integer> daysOfWeek, @RequestParam java.time.LocalTime startTime,
                                   @RequestParam java.time.LocalTime endTime, RedirectAttributes redirect) {
        AvailabilityRequest request = new AvailabilityRequest(daysOfWeek, startTime, endTime);
        return perform(redirect, () -> { validate(request); service.replaceAvailability(request); });
    }

    @PostMapping("/no-js/availability/clear")
    public String clearAvailability(RedirectAttributes redirect) { return perform(redirect, service::clearAvailability); }

    @PostMapping("/no-js/schedule")
    public String schedule(RedirectAttributes redirect) { return perform(redirect, service::generateSchedule); }

    @PostMapping("/no-js/blocks/{id}/completion")
    public String completion(@PathVariable Long id, @RequestParam boolean completed, RedirectAttributes redirect) {
        return perform(redirect, () -> service.setCompletion(id, completed));
    }

    private String perform(RedirectAttributes redirect, Runnable operation) {
        try { operation.run(); redirect.addFlashAttribute("message", "Alterações salvas."); }
        catch (RuntimeException exception) { redirect.addFlashAttribute("message", exception.getMessage() == null ? "Não foi possível salvar os dados." : exception.getMessage()); }
        return "redirect:/no-js";
    }

    private <T> void validate(T request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new IllegalArgumentException(violations.iterator().next().getMessage());
    }

    private static String subjectOptions(List<SubjectResponse> subjects, Long selected) {
        StringBuilder result = new StringBuilder();
        for (SubjectResponse subject : subjects) result.append("<option value=\"").append(subject.id()).append("\"").append(selected(selected != null && selected.equals(subject.id()))).append(">").append(escape(subject.name())).append("</option>");
        return result.toString();
    }

    private static String selected(boolean value) { return value ? " selected" : ""; }
    private static String dayName(int day) { return switch (day) { case 1 -> "Segunda-feira"; case 2 -> "Terça-feira"; case 3 -> "Quarta-feira"; case 4 -> "Quinta-feira"; case 5 -> "Sexta-feira"; case 6 -> "Sábado"; case 7 -> "Domingo"; default -> "Dia inválido"; }; }
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
