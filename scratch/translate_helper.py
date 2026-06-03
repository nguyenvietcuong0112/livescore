import os

langs = {
    "ar": {
        "assists_label": "التمريرات الحاسمة",
        "goals_label": "الأهداف",
        "realtime_match_radar": "رادار المباراة الفوري",
        "attack_momentum_pressure": "ضغط زخم الهجوم",
        "home_label": "المضيف",
        "away_label": "الضيف",
        "match_statistics": "إحصائيات المباراة",
        "ball_possession": "الاستحواذ على الكرة",
        "total_shots": "إجمالي التسديدات",
        "shots_on_target": "التسديدات على المرمى",
        "corner_kicks": "الضربات الركنية"
    },
    "de": {
        "assists_label": "VORLAGEN",
        "goals_label": "TORE",
        "realtime_match_radar": "ECHTZEIT-SPIELRADAR",
        "attack_momentum_pressure": "ANGRIFFSDRUCK",
        "home_label": "HEIM",
        "away_label": "GAST",
        "match_statistics": "SPIELSTATISTIKEN",
        "ball_possession": "Ballbesitz",
        "total_shots": "Schüsse insgesamt",
        "shots_on_target": "Torschüsse",
        "corner_kicks": "Eckbälle"
    },
    "es": {
        "assists_label": "ASISTENCIAS",
        "goals_label": "GOLES",
        "realtime_match_radar": "RADAR DE PARTIDO EN TIEMPO REAL",
        "attack_momentum_pressure": "PRESIÓN DEL MOMENTO DE ATAQUE",
        "home_label": "LOCAL",
        "away_label": "VISITANTE",
        "match_statistics": "ESTADÍSTICAS DEL PARTIDO",
        "ball_possession": "Posesión del balón",
        "total_shots": "Total de tiros",
        "shots_on_target": "Tiros al arco",
        "corner_kicks": "Tiros de esquina"
    },
    "fr": {
        "assists_label": "PASSES DÉCISIVES",
        "goals_label": "BUTS",
        "realtime_match_radar": "RADAR DE MATCH EN TEMPS RÉEL",
        "attack_momentum_pressure": "PRESSION OFFENSIVE DYNAMIQUE",
        "home_label": "DOMICILE",
        "away_label": "EXTÉRIEUR",
        "match_statistics": "STATISTIQUES DU MATCH",
        "ball_possession": "Possession du ballon",
        "total_shots": "Total des tirs",
        "shots_on_target": "Tirs cadrés",
        "corner_kicks": "Corners"
    },
    "hi": {
        "assists_label": "सहायता करता है",
        "goals_label": "गोल",
        "realtime_match_radar": "वास्तविक समय मैच रडार",
        "attack_momentum_pressure": "आक्रमण गति का दबाव",
        "home_label": "घरेलू",
        "away_label": "बाहरी",
        "match_statistics": "मैच के आँकड़े",
        "ball_possession": "गेंद पर नियंत्रण",
        "total_shots": "कुल शॉट",
        "shots_on_target": "लक्ष्य पर शॉट",
        "corner_kicks": "कॉर्नर किक"
    },
    "id": {
        "assists_label": "ASSISTS",
        "goals_label": "GOL",
        "realtime_match_radar": "RADAR PERTANDINGAN REAL-TIME",
        "attack_momentum_pressure": "TEKANAN MOMENTUM SERANGAN",
        "home_label": "KANDANG",
        "away_label": "TANDANG",
        "match_statistics": "STATISTIK PERTANDINGAN",
        "ball_possession": "Penguasaan Bola",
        "total_shots": "Total Tembakan",
        "shots_on_target": "Tembakan Tepat Sasaran",
        "corner_kicks": "Tendangan Sudut"
    },
    "it": {
        "assists_label": "ASSIST",
        "goals_label": "GOL",
        "realtime_match_radar": "RADAR PARTITA IN TEMPO REALE",
        "attack_momentum_pressure": "PRESSIONE MOMENTO DI ATTACCO",
        "home_label": "CASA",
        "away_label": "TRASFERTA",
        "match_statistics": "STATISTICHE PARTITA",
        "ball_possession": "Possesso palla",
        "total_shots": "Tiri totali",
        "shots_on_target": "Tiri in porta",
        "corner_kicks": "Calci d'angolo"
    },
    "ja": {
        "assists_label": "アシスト",
        "goals_label": "ゴール",
        "realtime_match_radar": "リアルタイム試合レーダー",
        "attack_momentum_pressure": "攻撃モメンタムの圧力",
        "home_label": "ホーム",
        "away_label": "アウェイ",
        "match_statistics": "試合統計",
        "ball_possession": "ボール支配率",
        "total_shots": "シュート数",
        "shots_on_target": "枠内シュート",
        "corner_kicks": "コーナーキック"
    },
    "pt": {
        "assists_label": "ASSISTÊNCIAS",
        "goals_label": "GOLS",
        "realtime_match_radar": "RADAR DE JOGO EM TEMPO REAL",
        "attack_momentum_pressure": "PRESSÃO DO MOMENTO DE ATAQUE",
        "home_label": "CASA",
        "away_label": "FORA",
        "match_statistics": "ESTATÍSTICAS DA PARTIDA",
        "ball_possession": "Posse de bola",
        "total_shots": "Total de chutes",
        "shots_on_target": "Chutes no gol",
        "corner_kicks": "Escanteios"
    },
    "ru": {
        "assists_label": "ГОЛЕВЫЕ ПЕРЕДАЧИ",
        "goals_label": "ГОЛЫ",
        "realtime_match_radar": "РАДАР МАТЧА В РЕАЛЬНОМ ВРЕМЕНИ",
        "attack_momentum_pressure": "ДАВЛЕНИЕ АТАКУЮЩЕГО ИМПУЛЬСА",
        "home_label": "ХОЗЯЕВА",
        "away_label": "ГОСТИ",
        "match_statistics": "СТАТИСТИКА МАТЧА",
        "ball_possession": "Владение мячом",
        "total_shots": "Всего ударов",
        "shots_on_target": "Удары в створ",
        "corner_kicks": "Угловые удары"
    },
    "th": {
        "assists_label": "แอสซิสต์",
        "goals_label": "ประตู",
        "realtime_match_radar": "เรดาร์การแข่งขันแบบเรียลไทม์",
        "attack_momentum_pressure": "แรงกดดันจากจังหวะการบุก",
        "home_label": "เหย้า",
        "away_label": "เยือน",
        "match_statistics": "สถิติการแข่งขัน",
        "ball_possession": "การครองบอล",
        "total_shots": "โอกาสยิงทั้งหมด",
        "shots_on_target": "ยิงเข้ากรอบ",
        "corner_kicks": "ลูกเตะมุม"
    },
    "tr": {
        "assists_label": "ASİSTLER",
        "goals_label": "GOLLER",
        "realtime_match_radar": "GERÇEK ZAMANLI MAÇ RADARI",
        "attack_momentum_pressure": "HÜCUM BASKISI",
        "home_label": "EV SAHİBİ",
        "away_label": "DEPLASMAN",
        "match_statistics": "MAÇ İSTATİSTİKLERİ",
        "ball_possession": "Topa Sahip Olma",
        "total_shots": "Toplam Şut",
        "shots_on_target": "Isabetli Şut",
        "corner_kicks": "Köşe Vuruşları"
    },
    "ur": {
        "assists_label": "اسسٹ",
        "goals_label": "گول",
        "realtime_match_radar": "ریئل ٹائم میچ راڈار",
        "attack_momentum_pressure": "حملہ آور رفتار کا دباؤ",
        "home_label": "ہوم",
        "away_label": "اوے",
        "match_statistics": "میچ کے اعدادوشمار",
        "ball_possession": "گیند کا قبضہ",
        "total_shots": "کل شاٹس",
        "shots_on_target": "نشانے پر شاٹس",
        "corner_kicks": "کارنر کِکس"
    }
}

res_path = "app/src/main/res"
for folder, translations in langs.items():
    strings_file = os.path.join(res_path, f"values-{folder}", "strings.xml")
    if not os.path.exists(strings_file):
        print(f"Skipping {folder}: file not found.")
        continue
    
    with open(strings_file, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check if closing tag exists
    if "</resources>" not in content:
        continue
        
    # Check if already added
    if "assists_label" in content:
        print(f"{folder} already has keys, skipping.")
        continue
        
    # Build replacement string
    lines = ["\n    <!-- Match Detail Stats, Radar & Top Players -->"]
    for key, value in translations.items():
        # Escape single quotes for Android XML
        escaped_value = value.replace("'", "\\'")
        lines.append(f'    <string name="{key}">{escaped_value}</string>')
    
    lines.append("\n</resources>")
    replacement = "\n".join(lines)
    
    new_content = content.replace("</resources>", replacement)
    
    with open(strings_file, "w", encoding="utf-8") as f:
        f.write(new_content)
    print(f"Updated {folder} strings.xml successfully!")
