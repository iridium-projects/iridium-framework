package cc.asylum.iridium.data.spec;

public sealed interface Op permits
    Op.Equal,
    Op.NotEqual,
    Op.EqualIgnoreCase,
    Op.Like,
    Op.LikeIgnoreCase,
    Op.NotLike,
    Op.GreaterThan,
    Op.GreaterThanOrEqual,
    Op.LessThan,
    Op.LessThanOrEqual,
    Op.In,
    Op.NotIn,
    Op.Null,
    Op.NotNull,
    Op.Between,
    Op.StartingWith,
    Op.EndingWith,
    Op.Empty,
    Op.NotEmpty,
    Op.True,
    Op.False {

  final class Equal implements Op {
    private Equal() {
    }
  }

  final class NotEqual implements Op {
    private NotEqual() {
    }
  }

  final class EqualIgnoreCase implements Op {
    private EqualIgnoreCase() {
    }
  }

  final class Like implements Op {
    private Like() {
    }
  }

  final class LikeIgnoreCase implements Op {
    private LikeIgnoreCase() {
    }
  }

  final class NotLike implements Op {
    private NotLike() {
    }
  }

  final class GreaterThan implements Op {
    private GreaterThan() {
    }
  }

  final class GreaterThanOrEqual implements Op {
    private GreaterThanOrEqual() {
    }
  }

  final class LessThan implements Op {
    private LessThan() {
    }
  }

  final class LessThanOrEqual implements Op {
    private LessThanOrEqual() {
    }
  }

  final class In implements Op {
    private In() {
    }
  }

  final class NotIn implements Op {
    private NotIn() {
    }
  }

  final class Null implements Op {
    private Null() {
    }
  }

  final class NotNull implements Op {
    private NotNull() {
    }
  }

  final class Between implements Op {
    private Between() {
    }
  }

  final class StartingWith implements Op {
    private StartingWith() {
    }
  }

  final class EndingWith implements Op {
    private EndingWith() {
    }
  }

  final class Empty implements Op {
    private Empty() {
    }
  }

  final class NotEmpty implements Op {
    private NotEmpty() {
    }
  }

  final class True implements Op {
    private True() {
    }
  }

  final class False implements Op {
    private False() {
    }
  }
}
