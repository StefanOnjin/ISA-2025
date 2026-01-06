package Jutjubic.RA56.validation;

import Jutjubic.RA56.dto.RegistrationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegistrationRequest> {
	@Override
	public boolean isValid(RegistrationRequest value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		boolean matches = Objects.equals(value.getPassword(), value.getConfirmPassword());
		if (!matches) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("Passwords do not match")
					.addPropertyNode("confirmPassword")
					.addConstraintViolation();
		}
		return matches;
	}
}
