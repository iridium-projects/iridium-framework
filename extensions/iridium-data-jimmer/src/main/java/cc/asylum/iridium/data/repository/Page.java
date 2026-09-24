package cc.asylum.iridium.data.repository;

import java.util.List;

public record Page<T>(
  List<T> content,
  int page,
  int size,
  long total
) {

  public Page {
    content = List.copyOf(content);

    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("size must be > 0");
    }
  }

  public int pages() {
    return (int) ((total + size - 1) / size);
  }
}
