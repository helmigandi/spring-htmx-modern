package com.modernfrontendshtmx.todomvc.todo.web;

import com.modernfrontendshtmx.todomvc.todo.TodoItem;
import com.modernfrontendshtmx.todomvc.todo.TodoItemNotFoundException;
import com.modernfrontendshtmx.todomvc.todo.TodoItemRepository;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class TodoItemController {

    private final TodoItemRepository repository;

    public TodoItemController(TodoItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String index(Model model) {
        addAttributesForIndex(model, ListFilter.ALL);
        return "index";
    }

    @GetMapping("/active")
    public String indexActive(Model model) {
        addAttributesForIndex(model, ListFilter.ACTIVE);
        return "index";
    }

    @GetMapping("/completed")
    public String indexCompleted(Model model) {
        addAttributesForIndex(model, ListFilter.COMPLETED);
        return "index";
    }

    @PostMapping
    public String addNewTodoItem(@Valid @ModelAttribute("item") TodoItemFormData formData) {
        repository.save(new TodoItem(formData.getTitle(), false));

        return "redirect:/";
    }

    // tag::htmxAddTodoItem[]
    @PostMapping
    @HxRequest
    @HxTrigger("itemAdded")
    public String htmxAddTodoItem(TodoItemFormData formData,
                                  Model model) { // <.>
        TodoItem item = repository.save(new TodoItem(formData.getTitle(), false));
        model.addAttribute("item", toDto(item));

        return "fragments :: todoItem";
    }
    // end::htmxAddTodoItem[]

    @PutMapping("/{id}/toggle")
    public String toggleSelection(@PathVariable("id") Long id) {
        TodoItem todoItem = repository.findById(id)
                .orElseThrow(() -> new TodoItemNotFoundException(id));

        todoItem.setCompleted(!todoItem.isCompleted());
        repository.save(todoItem);
        return "redirect:/";
    }

    @PutMapping("/toggle-all")
    public String toggleAll() {
        List<TodoItem> todoItems = repository.findAll();
        for (TodoItem todoItem : todoItems) {
            todoItem.setCompleted(!todoItem.isCompleted());
            repository.save(todoItem);
        }
        return "redirect:/";
    }

    @DeleteMapping("/{id}")
    public String deleteTodoItem(@PathVariable("id") Long id) {
        repository.deleteById(id);

        return "redirect:/";
    }

    @DeleteMapping("/completed")
    public String deleteCompletedItems() {
        List<TodoItem> items = repository.findAllByCompleted(true);
        for (TodoItem item : items) {
            repository.deleteById(item.getId());
        }
        return "redirect:/";
    }

    // tag::htmxActiveItemsCount[]
    @GetMapping("/active-items-count")
    @HxRequest
    public String htmxActiveItemsCount(Model model) {
        model.addAttribute("numberOfActiveItems", getNumberOfActiveItems());

        return "fragments :: active-items-count";
    }
    // end::htmxActiveItemsCount[]

    // tag::htmxToggleTodoItem[]
    @PutMapping("/{id}/toggle") // <.>
    @HxRequest
    @HxTrigger("itemCompletionToggled")
    public String htmxToggleTodoItem(@PathVariable("id") Long id,
                                     Model model) {
        TodoItem todoItem = repository.findById(id)
                .orElseThrow(() -> new TodoItemNotFoundException(id));

        todoItem.setCompleted(!todoItem.isCompleted());
        repository.save(todoItem);

        model.addAttribute("item", toDto(todoItem)); // <.>

        return "fragments :: todoItem"; // <.>
    }
    // end::htmxToggleTodoItem[]

    // tag::htmxDeleteTodoItem[]
    @DeleteMapping("/{id}")
    @HxRequest
    @ResponseBody //<.>
    @HxTrigger("itemDeleted") //<.>
    public String htmxDeleteTodoItem(@PathVariable("id") Long id) {
        repository.deleteById(id);

        return ""; //<.>
    }
    // end::htmxDeleteTodoItem[]

    private void addAttributesForIndex(Model model,
                                       ListFilter listFilter) {
        model.addAttribute("item", new TodoItemFormData());
        model.addAttribute("filter", listFilter);
        model.addAttribute("todos", getTodoItems(listFilter));
        model.addAttribute("totalNumberOfItems", repository.count());
        model.addAttribute("numberOfActiveItems", getNumberOfActiveItems());
        model.addAttribute("numberOfCompletedItems", getNumberOfCompletedItems());
    }

    private List<TodoItemDto> getTodoItems(ListFilter filter) {
        return switch (filter) {
            case ALL -> convertToDto(repository.findAll());
            case ACTIVE -> convertToDto(repository.findAllByCompleted(false));
            case COMPLETED -> convertToDto(repository.findAllByCompleted(true));
        };
    }

    private List<TodoItemDto> convertToDto(List<TodoItem> todoItems) {
        return todoItems
                .stream()
                .map(this::toDto)
                .toList();
    }

    private TodoItemDto toDto(TodoItem todoItem) {
        return new TodoItemDto(todoItem.getId(),
                todoItem.getTitle(),
                todoItem.isCompleted());
    }

    private int getNumberOfActiveItems() {
        return repository.countAllByCompleted(false);
    }

    private int getNumberOfCompletedItems() {
        return repository.countAllByCompleted(true);
    }

    public record TodoItemDto(long id, String title, boolean completed) {
    }

    public enum ListFilter {
        ALL,
        ACTIVE,
        COMPLETED
    }
}
