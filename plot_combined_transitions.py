import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import json
from pathlib import Path

# Create output directory
output_dir = Path("combined_phase_plots")
output_dir.mkdir(exist_ok=True)

# Read data
with open('phase_diagram_results.json', 'r') as f:
    data = json.load(f)
    df = pd.DataFrame(data['results'])

def plot_combined_view(metric, title, T_value):
    """Create a combined view with heatmap and line plots"""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(20, 8))
    
    # Heatmap
    data = df[df['temptation'] == T_value]
    pivot = data.pivot(
        index='defector_ratio',
        columns='density',
        values=metric
    )
    
    sns.heatmap(pivot, cmap='RdYlBu_r', annot=True, fmt='.2f',
                xticklabels=True, yticklabels=True, ax=ax1)
    
    ax1.set_title(f'Phase Diagram (T={T_value})')
    ax1.set_xlabel('Density (N/area)')
    ax1.set_ylabel('Defector Ratio')
    
    # Line plots for selected densities
    densities = [0.2, 0.5, 0.8]  # Selected densities
    
    for density in densities:
        data_slice = df[
            (df['temptation'] == T_value) & 
            (np.abs(df['density'] - density) < 0.01)
        ]
        ax2.plot(data_slice['defector_ratio'], data_slice[metric], 
                'o-', label=f'Density = {density}')
    
    ax2.set_xlabel('Defector Ratio')
    ax2.set_ylabel(metric)
    ax2.set_title(f'{title} vs Defector Ratio\n(T={T_value})')
    ax2.legend()
    ax2.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(output_dir / f'combined_{metric}_T{T_value}.png', dpi=300, bbox_inches='tight')
    plt.close()

# Plot combined views for different metrics and T values
metrics = {
    'avg_compliance': 'Average Compliance',
    'defector_percolation_prob': 'Defector Percolation Probability',
    'geometric_percolation_prob': 'Geometric Percolation Probability',
    'avg_largest_cluster_frac': 'Average Largest Cluster Fraction'
}

T_values = [1.1, 1.7, 2.0]  # Selected T values for comparison

for metric, title in metrics.items():
    for T in T_values:
        plot_combined_view(metric, title, T)

print("Combined phase transition plots have been saved to 'combined_phase_plots' directory.") 