package edu.sems.web;

import edu.sems.domain.Employee;
import edu.sems.domain.Role;
import edu.sems.service.EmployeeService;
import edu.sems.service.EmployeeValidationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public String list(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping("/employees/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("employeeForm")) {
            model.addAttribute("employeeForm", new CreateEmployeeForm());
        }
        model.addAttribute("roles", Role.values());
        return "employees/new";
    }

    @PostMapping("/employees")
    public String create(
            @Valid @ModelAttribute("employeeForm") CreateEmployeeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Employee employee = employeeService.create(
                        form.getEmployeeNumber(),
                        form.getFirstName(),
                        form.getLastName(),
                        form.getEmail(),
                        form.getJobTitle(),
                        form.getUsername(),
                        form.getPassword(),
                        form.getRole());
                redirectAttributes.addFlashAttribute("message", "Employee created.");
                return "redirect:/employees/" + employee.getId();
            } catch (EmployeeValidationException exception) {
                reject(bindingResult, exception);
            }
        }
        model.addAttribute("roles", Role.values());
        return "employees/new";
    }

    @GetMapping("/employees/{id}")
    public String view(@PathVariable long id, Model model) {
        model.addAttribute("employee", employeeService.findById(id));
        return "employees/view";
    }

    @GetMapping("/employees/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Employee employee = employeeService.findById(id);
        model.addAttribute("employee", employee);
        if (!model.containsAttribute("employeeForm")) {
            model.addAttribute("employeeForm", UpdateEmployeeForm.from(employee));
        }
        return "employees/edit";
    }

    @PostMapping("/employees/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("employeeForm") UpdateEmployeeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Employee employee = employeeService.findById(id);
        if (!bindingResult.hasErrors()) {
            try {
                employeeService.update(
                        id,
                        form.getFirstName(),
                        form.getLastName(),
                        form.getEmail(),
                        form.getJobTitle());
                redirectAttributes.addFlashAttribute("message", "Employee updated.");
                return "redirect:/employees/" + id;
            } catch (EmployeeValidationException exception) {
                reject(bindingResult, exception);
            }
        }
        model.addAttribute("employee", employee);
        return "employees/edit";
    }

    @PostMapping("/employees/{id}/deactivate")
    public String deactivate(
            @PathVariable long id,
            RedirectAttributes redirectAttributes) {
        employeeService.deactivate(id);
        redirectAttributes.addFlashAttribute("message", "Employee deactivated.");
        return "redirect:/employees/" + id;
    }

    private static void reject(BindingResult bindingResult, EmployeeValidationException exception) {
        if (bindingResult.getFieldType(exception.getField()) != null) {
            bindingResult.rejectValue(exception.getField(), "invalid", exception.getMessage());
        } else {
            bindingResult.reject("invalid", exception.getMessage());
        }
    }
}
