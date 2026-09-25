from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

labels = ["GET /health", "GET /products", "POST /orders"]
iri = [239004, 169393, 133480]
qrk = [290034, 266665, 107881]
spr = [153312, 126192, 99417]
iri_p50 = [0.90, 1.24, 1.60]
qrk_p50 = [0.65, 0.72, 1.94]
spr_p50 = [1.37, 1.65, 2.53]
startup = [449, 991, 2284]
names = ["Iridium", "Quarkus 3.39.4", "Spring Boot 4.1.1"]
colors = ["#5b8def", "#e23d3d", "#6db33f"]

fig, axes = plt.subplots(1, 3, figsize=(13.4, 4.6), constrained_layout=True)
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

bars = axes[0].bar(names, startup, color=colors, width=0.62)
axes[0].bar_label(bars, fmt="%.0f", padding=3, color="#d7dbe3", fontsize=8)
axes[0].set_title("Startup")
axes[0].set_ylabel("median ms")
axes[0].tick_params(axis="x", labelsize=8)
axes[0].set_ylim(0, 2800)

x = np.arange(len(labels))
width = 0.25
series = [(iri, "Iridium", colors[0]), (qrk, "Quarkus", colors[1]), (spr, "Spring", colors[2])]
for i, (vals, label, color) in enumerate(series):
    b = axes[1].bar(x + (i - 1) * width, [v / 1000 for v in vals], width, label=label, color=color)
    axes[1].bar_label(b, labels=[f"{v/1000:.0f}" for v in vals], padding=2, color="#d7dbe3", fontsize=7)
axes[1].set_title("Throughput")
axes[1].set_ylabel("thousand requests / s")
axes[1].set_xticks(x, labels, fontsize=8)
axes[1].legend(frameon=False, labelcolor="#d7dbe3", fontsize=8)
axes[1].set_ylim(0, 360)

p50 = [iri_p50, qrk_p50, spr_p50]
for i, (vals, label, color) in enumerate(zip(p50, ["Iridium", "Quarkus", "Spring"], colors)):
    axes[2].bar(x + (i - 1) * width, vals, width, label=label, color=color)
axes[2].set_title("Latency p50")
axes[2].set_ylabel("ms")
axes[2].set_xticks(x, labels, fontsize=8)
axes[2].legend(frameon=False, labelcolor="#d7dbe3", fontsize=8)
axes[2].set_ylim(0, 3.4)

fig.suptitle("Shop API  ·  same load: wrk 12 threads, 256 connections, 10s", color="#f4f6fb", fontsize=13)
out = Path("/home/ly/Projects/iridium-framework/demos/bench-hello/shop-three.png")
fig.savefig(out, dpi=160)
print(out)
