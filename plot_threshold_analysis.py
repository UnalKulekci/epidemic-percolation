import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path
from scipy.signal import find_peaks
from scipy.ndimage import gaussian_filter1d

# Çıktı klasörü oluştur
output_dir = Path("threshold_analysis")
output_dir.mkdir(exist_ok=True)

# Veriyi oku
df = pd.read_csv('phase_diagram_results.csv')

def find_threshold(x, y, threshold=0.5):
    """Verilen eşik değerini geçtiği noktayı bul"""
    # Veriyi düzgünleştir
    y_smooth = gaussian_filter1d(y, sigma=1)
    try:
        idx = np.where(y_smooth >= threshold)[0][0]
        return x[idx]
    except IndexError:
        return None

def plot_threshold_curves(metric, title, threshold=0.5):
    """Belirli bir metrik için eşik analizi grafikleri"""
    plt.figure(figsize=(15, 10))
    
    # Her defector oranı için ayrı çizgi
    defector_ratios = sorted(df['defectorRatio'].unique())
    colors = plt.cm.viridis(np.linspace(0, 1, len(defector_ratios)))
    
    thresholds = []
    
    for ratio, color in zip(defector_ratios, colors):
        # T=1.7 için filtrele (daha belirgin faz geçişi için)
        data = df[(df['defectorRatio'] == ratio) & (df['temptation'] == 1.7)]
        x = data['density'].values
        y = data[metric].values
        
        # Veriyi düzgünleştir
        y_smooth = gaussian_filter1d(y, sigma=1)
        plt.plot(x, y_smooth, '-', color=color, alpha=0.7)
        plt.plot(x, y, 'o', label=f'Defector Ratio = {ratio:.1f}', color=color)
        
        # Eşik noktasını bul ve işaretle
        thresh = find_threshold(x, y_smooth, threshold)
        if thresh is not None:
            plt.axvline(x=thresh, color=color, linestyle='--', alpha=0.3)
            plt.plot(thresh, threshold, 'x', color=color, markersize=10)
            thresholds.append((ratio, thresh))
    
    plt.grid(True, alpha=0.3)
    plt.xlabel('Density (N/area)')
    plt.ylabel(metric)
    plt.title(f'{title}\nT=1.7, Threshold={threshold}')
    plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
    
    # Dosyaya kaydet
    filename = f"threshold_{metric}_T1.7.png"
    plt.savefig(output_dir / filename, dpi=300, bbox_inches='tight')
    plt.close()
    
    return thresholds

def plot_density_curves(metric, title):
    """Yoğunluğa göre değişim grafikleri"""
    plt.figure(figsize=(15, 10))
    
    # Her temptation değeri için ayrı çizgi
    temptation_values = sorted(df['temptation'].unique())
    colors = plt.cm.viridis(np.linspace(0, 1, len(temptation_values)))
    
    for t, color in zip(temptation_values, colors):
        # defector ratio = 0.5 için filtrele
        data = df[(df['defectorRatio'] == 0.5) & (df['temptation'] == t)]
        x = data['density'].values
        y = data[metric].values
        
        # Veriyi düzgünleştir
        y_smooth = gaussian_filter1d(y, sigma=1)
        plt.plot(x, y_smooth, '-', color=color, alpha=0.7)
        plt.plot(x, y, 'o', label=f'T = {t}', color=color)
        
        # Değişimin en hızlı olduğu noktaları bul
        dy = np.gradient(y_smooth, x)
        peaks, _ = find_peaks(np.abs(dy), prominence=0.1)
        if len(peaks) > 0:
            plt.plot(x[peaks], y[peaks], 'x', color=color, markersize=10)
    
    plt.grid(True, alpha=0.3)
    plt.xlabel('Density (N/area)')
    plt.ylabel(metric)
    plt.title(f'{title}\nDefector Ratio = 0.5')
    plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
    
    # Dosyaya kaydet
    filename = f"density_curves_{metric}_DR0.5.png"
    plt.savefig(output_dir / filename, dpi=300, bbox_inches='tight')
    plt.close()

def plot_phase_space(metric, title):
    """T vs Density faz uzayı"""
    plt.figure(figsize=(12, 10))
    
    # Defector ratio = 0.5 için
    data = df[df['defectorRatio'] == 0.5].pivot(
        index='temptation',
        columns='density',
        values=metric
    )
    
    plt.imshow(
        data,
        aspect='auto',
        origin='lower',
        extent=[data.columns.min(), data.columns.max(),
                data.index.min(), data.index.max()],
        cmap='RdYlBu'
    )
    
    plt.colorbar(label=metric)
    plt.xlabel('Density (N/area)')
    plt.ylabel('Temptation (T)')
    plt.title(f'{title}\nDefector Ratio = 0.5')
    
    # Dosyaya kaydet
    filename = f"phase_space_{metric}_DR0.5.png"
    plt.savefig(output_dir / filename, dpi=300, bbox_inches='tight')
    plt.close()

# Metrikleri analiz et
metrics = {
    'defectorPercolationProb': 'Defector Percolation Probability',
    'geometricPercolationProb': 'Geometric Percolation Probability',
    'avgLargestClusterFrac': 'Largest Defector Cluster Fraction',
    'avgCompliance': 'Final Compliance Rate'
}

# Her metrik için analizler
for metric, title in metrics.items():
    # Perkolasyon için 0.5 eşiği
    if 'Percolation' in title:
        thresholds = plot_threshold_curves(metric, title, threshold=0.5)
        print(f"\nThresholds for {title}:")
        for ratio, thresh in thresholds:
            if thresh is not None:
                print(f"Defector Ratio {ratio:.1f}: Density threshold at {thresh:.2f}")
    
    # Yoğunluk eğrileri
    plot_density_curves(metric, title)
    
    # Faz uzayı
    plot_phase_space(metric, title)

print("\nGrafikler 'threshold_analysis' klasörüne kaydedildi.") 