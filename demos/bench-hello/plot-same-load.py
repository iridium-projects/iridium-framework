from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

labels = ["GET /health", "GET /products", "POST /orders"]
iri_rps = [239004, 169393, 133480]
spr_rps = [153312, 126192, 99417]
iri_p50 = [0.90, 1.24, 1.60]
spr_p50 = [1.37, 1.65, 2.53]
iri_p99 = [8.81, 11.90, 14.73]
spr_p99 = [8.64, 66.92, 323.31]

fig, axes = plt.subplots(1, 2, figsize=(11.2, 4.8), constrained_layout=True)
fig.patch.set_facecolor("#0f1115")
for ax in axes:
    ax.set_facecolor("#171a21")
    ax.tick_params(colors="#d7dbe3")
    ax.yaxis.label.set_color("#d7dbe3")
    ax.title.set_color("#f4f6fb")
    for spine in ax.spines.values():
        spine.set_color("#2a303b")
    ax.grid(axis="y", color="#2a303b", linewidth=0.6)
    ax.set_axisbelow(True)

x = np.arange(len(labels))
width = 0.36
iri_color = "#5b8def"
spr_color = "#6db33f"

b1 = axes[0].bar(x - width / 2, [v / 1000 for v in iri_rps], width, label="Iridium", color=iri_color)
b2 = axes[0].bar(x + width / 2, [v / 1000 for v in spr_rps], width, label="Spring Boot 4.1.1", color=spr_color)
axes[0].bar_label(b1, labels=[f"{v/1000:.0f}k" for v in iri_rps], padding=3, color="#d7dbe3", fontsize=8)
axes[0].bar_label(b2, labels=[f"{v/1000:.0f}k" for v in spr_rps], padding=3, color="#d7dbe3", fontsize=8)
axes[0].set_title("Throughput")
axes[0].set_ylabel("thousand requests / s")
axes[0].set_xticks(x, labels)
axes[0].legend(frameon=False, labelcolor="#d7dbe3")
axes[0].set_ylim(0, 290)

axes[1].bar(x - width / 2, iri_p50, width, label="Iridium p50", color=iri_color)
axes[1].bar(x + width / 2, spr_p50, width, label="Spring p50", color=spr_color)
axes[1].set_title("Latency p50")
axes[1].set_ylabel("ms")
axes[1].set_xticks(x, labels)
axes[1].legend(frameon=False, labelcolor="#d7dbe3")
axes[1].set_ylim(0, 3.4)
for i, (a, b) in enumerate(zip(iri_p99, spr_p99)):
    axes[1].annotate(f"p99 {a:.1f}", (i - width / 2, iri_p50[i]), textcoords="offset points", xytext=(0, 8), ha="center", color="#9ec1ff", fontsize=7)
    note = f"p99 {b:.0f}" if b > 20 else f"p99 {b:.1f}"
    axes[1].annotate(note, (i + width / 2, spr_p50[i]), textcoords="offset points", xytext=(0, 8), ha="center", color="#b7e39a", fontsize=7)

fig.suptitle("Same load  ·  wrk 12 threads, 256 connections, 10s", color="#f4f6fb", fontsize=13)
out = Path("/home/ly/Projects/iridium-framework/demos/bench-hello/shop-same-load.png")
fig.savefig(out, dpi=160)
print(out)
