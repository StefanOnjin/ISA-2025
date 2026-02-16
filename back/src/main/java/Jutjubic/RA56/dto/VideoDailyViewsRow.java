package Jutjubic.RA56.dto;

import java.time.LocalDate;

public interface VideoDailyViewsRow {
	Long getVideoId();
	LocalDate getViewDate();
	Long getViewsCount();
}
