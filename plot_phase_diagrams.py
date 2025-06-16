import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Çıktı klasörü oluştur
output_dir = Path("phase_diagrams")
output_dir.mkdir(exist_ok=True)

# Veriyi oku
df = pd.read_csv('phase_diagram_results.csv')

def plot_density_phase_diagram(metric, title, cmap='viridis', t_value=1.3):
    """Yoğunluk vs Defektör Oranı faz diyagramı"""
    df_filtered = df[df['temptation'] == t_value]
    
    pivot = df_filtered.pivot(
        index='defectorRatio', 
        columns='density',
        values=metric
    )
    
    plt.figure(figsize=(12, 10))
    sns.heatmap(pivot, cmap=cmap, annot=True, fmt='.2f', 
                xticklabels=True, yticklabels=True)
    plt.title(f'{title} (T={t_value})')
    plt.xlabel('Density (N/area)')
    plt.ylabel('Initial Defector Ratio')
    
    # Dosyaya kaydet
    filename = f"phase_density_{metric}_T{t_value}.png"
    plt.savefig(output_dir / filename, dpi=300, bbox_inches='tight')
    plt.close()

def plot_temptation_phase_diagram(metric, title, cmap='viridis', n_value=80):
    """Temptation vs Defektör Oranı faz diyagramı"""
    df_filtered = df[df['agentCount'] == n_value]
    
    pivot = df_filtered.pivot(
        index='defectorRatio',
        columns='temptation',
        values=metric
    )
    
    plt.figure(figsize=(12, 10))
    sns.heatmap(pivot, cmap=cmap, annot=True, fmt='.2f',
                xticklabels=True, yticklabels=True)
    plt.title(f'{title} (N={n_value})')
    plt.xlabel('Temptation (T)')
    plt.ylabel('Initial Defector Ratio')
    
    # Dosyaya kaydet
    filename = f"phase_temptation_{metric}_N{n_value}.png"
    plt.savefig(output_dir / filename, dpi=300, bbox_inches='tight')
    plt.close()

# Farklı T değerleri için yoğunluk faz diyagramları
for t in [1.1, 1.3, 1.5]:
    # Final Compliance Rate
    plot_density_phase_diagram(
        'avgCompliance',
        'Final Compliance Rate',
        cmap='RdYlBu',
        t_value=t
    )
    
    # Defector Percolation Probability
    plot_density_phase_diagram(
        'defectorPercolationProb',
        'Defector Percolation Probability',
        cmap='RdYlBu_r',
        t_value=t
    )
    
    # Largest Cluster Fraction
    plot_density_phase_diagram(
        'avgLargestClusterFrac',
        'Largest Defector Cluster Fraction',
        cmap='RdYlBu_r',
        t_value=t
    )
    
    # Geometric Percolation Probability
    plot_density_phase_diagram(
        'geometricPercolationProb',
        'Geometric Percolation Probability',
        cmap='RdYlBu_r',
        t_value=t
    )

# Farklı N değerleri için temptation faz diyagramları
for n in [40, 60, 80]:
    # Final Compliance Rate
    plot_temptation_phase_diagram(
        'avgCompliance',
        'Final Compliance Rate',
        cmap='RdYlBu',
        n_value=n
    )
    
    # Defector Percolation Probability
    plot_temptation_phase_diagram(
        'defectorPercolationProb',
        'Defector Percolation Probability',
        cmap='RdYlBu_r',
        n_value=n
    )
    
    # Largest Cluster Fraction
    plot_temptation_phase_diagram(
        'avgLargestClusterFrac',
        'Largest Defector Cluster Fraction',
        cmap='RdYlBu_r',
        n_value=n
    )
    
    # Geometric Percolation Probability
    plot_temptation_phase_diagram(
        'geometricPercolationProb',
        'Geometric Percolation Probability',
        cmap='RdYlBu_r',
        n_value=n
    )

print("Faz diyagramları 'phase_diagrams' klasörüne kaydedildi.") 