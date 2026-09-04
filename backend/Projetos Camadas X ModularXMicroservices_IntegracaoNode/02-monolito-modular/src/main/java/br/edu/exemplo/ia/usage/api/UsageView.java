package br.edu.exemplo.ia.usage.api;
import java.time.Instant;
import java.util.UUID;
public record UsageView(UUID id,UUID projectId,long tokens,String model,Instant occurredAt){

}