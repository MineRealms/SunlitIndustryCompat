"""Render README equations as static images for Markdown renderers without LaTeX support."""
from pathlib import Path

import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / 'docs' / 'formulas'
FORMULAS = {
    'benchmark-ratios': (
        r'S=\frac{\mathrm{median}(t_{\mathrm{before}})}{\mathrm{median}(t_{\mathrm{after}})}',
        r'R_A=1-\frac{\mathrm{median}(a_{\mathrm{after}})}{\mathrm{median}(a_{\mathrm{before}})}'
        r'\quad(a_{\mathrm{before}}>0)',
    ),
    'allocation-rate': (r'\dot{A}=\sum_i f_i a_i',),
    'handler-signature': (r'\Sigma(H)=[(\mathrm{id}(h_i),g_i,b_i,f_i)]_{i=1}^{|H|}',),
    'cache-validity': (
        r'\mathrm{valid}(e)=(e.M\equiv M_{\mathrm{current}})'
        r'\wedge(e.\varepsilon=\varepsilon_{\mathrm{current}})',
    ),
    'snapshot-weight': (r'W=\sum_{p\in P}(96+2|p|)+\sum_{p\in B}(24+|\mathrm{bytes}_p|)',),
    'recipe-complexity': (r'O(QN)\quad\longrightarrow\quad O(N+QK)',),
    'bounds-distance': (
        r'\delta_i=\max(l_i-p_i,\,0,\,p_i-u_i)',
        r'd^2=\delta_x^2+\delta_y^2+\delta_z^2',
    ),
    'triangle-budget': (r'N_{\triangle}=2N_{\theta}N_{\phi}',),
    'ring-endpoints': (
        r'\theta_i=\varphi+\frac{2\pi i}{96}',
        r'v_i^{\pm}=(r\pm w)(\cos\theta_i,\,0,\,\sin\theta_i)',
    ),
    'quadratic-bezier': (r'B(t)=(1-t)^2P_0+2(1-t)tP_1+t^2P_2',),
}


def render():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, lines in FORMULAS.items():
        figure = plt.figure(figsize=(1, 1), dpi=160)
        for index, line in enumerate(lines):
            figure.text(0, -index * 0.52, '$' + line + '$', fontsize=16, color='#1f2328')
        figure.savefig(OUTPUT / (name + '.png'), bbox_inches='tight', pad_inches=0.12, facecolor='white')
        plt.close(figure)
    print('Rendered', len(FORMULAS), 'README equations in', OUTPUT)


if __name__ == '__main__':
    render()
