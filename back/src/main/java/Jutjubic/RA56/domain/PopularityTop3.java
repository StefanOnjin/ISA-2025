package Jutjubic.RA56.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "popularity_top3")
public class PopularityTop3 {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "top1_video_id")
	private Video top1Video;

	@Column(name = "top1_score")
	private Long top1Score;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "top2_video_id")
	private Video top2Video;

	@Column(name = "top2_score")
	private Long top2Score;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "top3_video_id")
	private Video top3Video;

	@Column(name = "top3_score")
	private Long top3Score;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Video getTop1Video() {
		return top1Video;
	}

	public void setTop1Video(Video top1Video) {
		this.top1Video = top1Video;
	}

	public Long getTop1Score() {
		return top1Score;
	}

	public void setTop1Score(Long top1Score) {
		this.top1Score = top1Score;
	}

	public Video getTop2Video() {
		return top2Video;
	}

	public void setTop2Video(Video top2Video) {
		this.top2Video = top2Video;
	}

	public Long getTop2Score() {
		return top2Score;
	}

	public void setTop2Score(Long top2Score) {
		this.top2Score = top2Score;
	}

	public Video getTop3Video() {
		return top3Video;
	}

	public void setTop3Video(Video top3Video) {
		this.top3Video = top3Video;
	}

	public Long getTop3Score() {
		return top3Score;
	}

	public void setTop3Score(Long top3Score) {
		this.top3Score = top3Score;
	}
}
