import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import seaborn as sns
from pathlib import Path
from scipy.ndimage import gaussian_filter1d

# Çıktı klasörü oluştur
output_dir = Path("improved_visualizations")
output_dir.mkdir(exist_ok=True)

# Veriyi oku
df = pd.read_csv('phase_diagram_results.csv')

def plot_subplots_by_groups(metric, title, t_value=1.7):
    """Defektör oranlarını gruplara ayırarak subplot'lar oluştur"""
    # Veriyi filtrele
    data = df[df['temptation'] == t_value]
    
    # Defektör oranlarını grupla
    low = [0.1, 0.2, 0.3]
    mid = [0.4, 0.5, 0.6]
    high = [0.7, 0.8, 0.9]
    groups = [('Low', low), ('Medium', mid), ('High', high)]
    
    fig, axes = plt.subplots(1, 3, figsize=(20, 6))
    fig.suptitle(f'{title} (T={t_value})', fontsize=16, y=1.05)
    
    for (name, ratios), ax in zip(groups, axes):
        for ratio in ratios:
            subset = data[data['defectorRatio'] == ratio]
            x = subset['density'].values
            y = subset[metric].values
            y_smooth = gaussian_filter1d(y, sigma=1)
            
            ax.plot(x, y_smooth, '-', label=f'Ratio = {ratio}')
            ax.plot(x, y, 'o', alpha=0.5)
            
            # 0.5 eşiği
            if metric.endswith('Prob'):
                thresh_idx = np.where(y_smooth >= 0.5)[0]
                if len(thresh_idx) > 0:
                    ax.axvline(x=x[thresh_idx[0]], color='gray', linestyle='--', alpha=0.3)
        
        ax.set_title(f'{name} Defector Ratios')
        ax.set_xlabel('Density (N/area)')
        ax.set_ylabel(metric)
        ax.grid(True, alpha=0.3)
        ax.legend()
    
    plt.tight_layout()
    plt.savefig(output_dir / f'subplots_{metric}_T{t_value}.png', dpi=300, bbox_inches='tight')
    plt.close()

def plot_heatmap(metric, title, t_value=1.7):
    """Isı haritası görselleştirmesi"""
    # Veriyi pivot table'a dönüştür
    data = df[df['temptation'] == t_value]
    pivot = data.pivot(
        index='defectorRatio',
        columns='density',
        values=metric
    )
    
    plt.figure(figsize=(12, 8))
    sns.heatmap(pivot, cmap='RdYlBu_r', annot=True, fmt='.2f',
                xticklabels=True, yticklabels=True)
    
    plt.title(f'{title}\nT={t_value}')
    plt.xlabel('Density (N/area)')
    plt.ylabel('Defector Ratio')
    
    plt.savefig(output_dir / f'heatmap_{metric}_T{t_value}.png', dpi=300, bbox_inches='tight')
    plt.close()

def plot_3d_surface(metric, title, t_value=1.7):
    """3D yüzey grafiği"""
    data = df[df['temptation'] == t_value]
    
    # Mesh grid oluştur
    X = data['density'].unique()
    Y = data['defectorRatio'].unique()
    X, Y = np.meshgrid(X, Y)
    
    # Z değerlerini hesapla
    pivot = data.pivot(
        index='defectorRatio',
        columns='density',
        values=metric
    )
    Z = pivot.values
    
    # 3D plot
    fig = plt.figure(figsize=(12, 8))
    ax = fig.add_subplot(111, projection='3d')
    
    # Yüzeyi çiz
    surf = ax.plot_surface(X, Y, Z, cmap='viridis',
                          linewidth=0, antialiased=True)
    
    # Konturları ekle
    ax.contour(X, Y, Z, zdir='z', offset=Z.min(), cmap='viridis')
    
    # Görsel ayarlar
    ax.set_xlabel('Density (N/area)')
    ax.set_ylabel('Defector Ratio')
    ax.set_zlabel(metric)
    ax.set_title(f'{title}\nT={t_value}')
    
    # Colorbar ekle
    fig.colorbar(surf, ax=ax, shrink=0.5, aspect=5)
    
    plt.savefig(output_dir / f'surface3d_{metric}_T{t_value}.png', dpi=300, bbox_inches='tight')
    plt.close()

# Metrikleri analiz et
metrics = {
    'geometricPercolationProb': 'Geometric Percolation Probability',
    'defectorPercolationProb': 'Defector Percolation Probability',
    'avgLargestClusterFrac': 'Largest Cluster Fraction',
    'avgCompliance': 'Final Compliance Rate'
}

# Her metrik için üç farklı görselleştirme
for metric, title in metrics.items():
    print(f"\nCreating visualizations for {title}...")
    
    # 1. Gruplu subplot'lar
    plot_subplots_by_groups(metric, title)
    
    # 2. Isı haritası
    plot_heatmap(metric, title)
    
    # 3. 3D yüzey
    plot_3d_surface(metric, title)

print("\nTüm görselleştirmeler 'improved_visualizations' klasörüne kaydedildi.") 