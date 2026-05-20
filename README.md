# TP Intergiciels

## Equipe
22201911: KERBELLEC **Alphonse**, alphonse.kerbellec@uphf.fr
<br>
22101912: VERZELE **Florian**, florian.verzele@uphf.fr
<br>

## Introduction
Ce projet met en place une messagerie basée sur Docker, et utilisée via le terminal, offrant une fonctionnalité de traduction automatique anglais -> français pour ses utilisateurs.

Pour utiliser la librairie, il suffit de suivre ces commandes:
```
> cd [dossier-au-choix]
> git clone https://github.com/Alias10294/tp_intergiciels.git
> docker compose up --build -d
```
Une fois les conteneurs mis en place, pour lancer un client de messagerie, il suffit d'utiliser dans un terminal à la racine du projet:
- Sur Windows
```
> .\start_chatting.bat [pseudo-au-choix] [groupe-au-choix](optionnel)
```
- Sur Linux/MacOS
```
> ./start_chatting.sh [pseudo-au-choix] [group-au-choix](optionnel)
```