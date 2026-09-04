package br.edu.exemplo.ia.usage.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/usages")
public class UsageController {
    private final UsageFacade facade;
    public UsageController(UsageFacade facade){this.facade=facade;
    }
    record Request(@NotNull UUID projectId,@Positive long tokens,@NotBlank String model){

    }

    @PostMapping
    UsageView register(@Valid @RequestBody Request r){
        return facade.register(new RegisterUsageCommand(r.projectId(),r.tokens(),r.model()));
    }
}