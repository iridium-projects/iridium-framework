from pathlib import Path

import matplotlib.pyplot as plt

conns = [64, 256, 1024, 4096]
iri_health = [183207, 304937, 308809, 298203]
qrk_health = [187693, 289290, 233439, 213598]
spr_health = [128375, 136408, 160521, 180272]
iri_product = [126598, 215291, 206886, 220758]
qrk_product = [199688, 263566, 237097, 188709]
spr_product = [107625, 132172, 150482, 160278]

fig, ax = plt.subplots(figsize=(9.4, 4.8), constrained_layout=True)
fig.patch.set_facecolor("#0f1115")
ax.set_facecolor("#171a21")
ax.tick_params(colors="#d7dbe3")
ax.yaxis.label.set_color("#d7dbe3")
ax.xaxis.label.set_color("#d7dbe3")
ax.title.set_color("#f4f6fb")
for spine in ax.spines.values():
    spine.set_color("#2a303b")
ax.grid(axis="y", color="#2a303b", linewidth=0.6)
ax.set_axisbelow(True)
ax.axhline(100_000, color="#8b93a7", linestyle=":", linewidth=0.8)

ax.plot(conns, [v / 1000 for v in iri_health], color="#5b8def", marker="o", linewidth=2, label="Iridium /health")
ax.plot(conns, [v / 1000 for v in qrk_health], color="#e23d3d", marker="o", linewidth=2, label="Quarkus /health")
ax.plot(conns, [v / 1000 for v in spr_health], color="#6db33f", marker="o", linewidth=2, label="Spring /health")
ax.plot(conns, [v / 1000 for v in iri_product], color="#5b8def", marker="s", linewidth=1.3, linestyle="--", label="Iridium /products")
ax.plot(conns, [v / 1000 for v in qrk_product], color="#e23d3d", marker="s", linewidth=1.3, linestyle="--", label="Quarkus /products")
ax.plot(conns, [v / 1000 for v in spr_product], color="#6db33f", marker="s", linewidth=1.3, linestyle="--", label="Spring /products")

ax.set_xscale("log")
ax.set_xticks(conns, [str(c) for c in conns])
ax.set_xlabel("concurrent connections")
ax.set_ylabel("thousand requests / s")
ax.set_title("Shop API load  ·  wrk, 12 threads, 8s")
ax.legend(frameon=False, labelcolor="#d7dbe3", fontsize=8, ncol=2)
ax.set_ylim(0, 380)

out = Path("/home/ly/Projects/iridium-framework/demos/bench-hello/shop-load-three.png")
fig.savefig(out, dpi=160)
print(out)
