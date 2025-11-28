package com.example.admin.permission.check;

import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.service.RowConditionEvaluator;
import org.springframework.stereotype.Component;

@Component
public class RowConditionPermissionCheck implements PermissionCheck {

  private final RowConditionEvaluator rowConditionEvaluator;

  public RowConditionPermissionCheck(RowConditionEvaluator rowConditionEvaluator) {
    this.rowConditionEvaluator = rowConditionEvaluator;
  }

  @Override
  public void check(PermissionEvaluationContext context) {
    boolean allowed =
        rowConditionEvaluator.isAllowed(
            context.assignment().getRowConditionExpression().orElse(null), context.attributes());
    if (!allowed) {
      throw new PermissionDeniedException("행 조건을 만족하지 않습니다.");
    }
  }
}
