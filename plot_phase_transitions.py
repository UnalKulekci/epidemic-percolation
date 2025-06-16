import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import json
from pathlib import Path

# Create output directory
output_dir = Path("phase_transition_plots")
output_dir.mkdir(exist_ok=True)

# Read data
with open('phase_diagram_results.json', 'r') as f:
    data = json.load(f)
    df = pd.DataFrame(data['results'])

def plot_phase_diagram_heatmaps(metric, title):
    """Create heatmaps for different T values showing phase transitions"""
    fig, axes = plt.subplots(2, 3, figsize=(20, 12))
    axes = axes.flatten()
    
    T_values = sorted(df['temptation'].unique())
    
    for i, T in enumerate(T_values):
        data = df[df['temptation'] == T]
        pivot = data.pivot(
            index='defector_ratio',
            columns='density',
            values=metric
        )
        
        sns.heatmap(pivot, cmap='RdYlBu_r', annot=False, fmt='.2f',
                    xticklabels=True, yticklabels=True, ax=axes[i])
        
        axes[i].set_title(f'T = {T}')
        axes[i].set_xlabel('Density (N/area)')
        axes[i].set_ylabel('Defector Ratio')
    
    if len(axes) > len(T_values):
        for i in range(len(T_values), len(axes)):
            fig.delaxes(axes[i])
    
    plt.suptitle(f'{title} Phase Diagram', fontsize=16, y=1.02)
    plt.tight_layout()
    plt.savefig(output_dir / f'phase_diagram_{metric}.png', dpi=300, bbox_inches='tight')
    plt.close()

def plot_metric_vs_defector_ratio(metric, title):
    """Plot metric vs defector ratio for different densities"""
    plt.figure(figsize=(12, 8))
    
    densities = [0.2, 0.5, 0.8]  # Selected densities
    T = 1.7  # Fixed T value
    
    for density in densities:
        data = df[
            (df['temptation'] == T) & 
            (np.abs(df['density'] - density) < 0.01)
        ]
        plt.plot(data['defector_ratio'], data[metric], 
                'o-', label=f'Density = {density}')
    
    plt.xlabel('Defector Ratio')
    plt.ylabel(metric)
    plt.title(f'{title} vs Defector Ratio (T={T})')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.savefig(output_dir / f'{metric}_vs_defector_ratio.png', dpi=300, bbox_inches='tight')
    plt.close()

# Plot phase diagrams for different metrics
metrics = {
    'avg_compliance': 'Average Compliance',
    'defector_percolation_prob': 'Defector Percolation Probability',
    'geometric_percolation_prob': 'Geometric Percolation Probability',
    'avg_largest_cluster_frac': 'Average Largest Cluster Fraction'
}

for metric, title in metrics.items():
    plot_phase_diagram_heatmaps(metric, title)
    plot_metric_vs_defector_ratio(metric, title)

print("Phase transition plots have been saved to 'phase_transition_plots' directory.") 