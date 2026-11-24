package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.expense.ExpenseRespDTO;
import com.mycropdiary.api.entity.cultivation.Expense;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExpenseMapper {

    public ExpenseRespDTO toResponse(Expense entity) {
        return ExpenseRespDTO.fromEntity(entity);
    }

    public List<ExpenseRespDTO> toResponseList(List<Expense> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
