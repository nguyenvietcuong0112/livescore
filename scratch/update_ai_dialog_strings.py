import os
import re

translations = {
    'values': {
        'dialog_ai_title': 'See What AI Predicts',
        'dialog_ai_subtitle': 'Unlock detailed score predictions before the match starts.',
        'dialog_ai_feature_1_title': 'Predicted final score',
        'dialog_ai_feature_1_desc': 'See the exact score AI predicts',
        'dialog_ai_feature_2_title': 'Winning probability',
        'dialog_ai_feature_2_desc': 'Win / Draw / Lose probability analysis',
        'dialog_ai_feature_3_title': 'Confidence level',
        'dialog_ai_feature_3_desc': 'AI confidence rating for every prediction',
        'dialog_ai_btn_later': 'Later',
        'dialog_ai_btn_go_premium': 'Go Premium'
    },
    'values-vi': {
        'dialog_ai_title': 'Xem dự đoán từ AI',
        'dialog_ai_subtitle': 'Mở khóa dự đoán tỷ số chi tiết trước khi trận đấu bắt đầu.',
        'dialog_ai_feature_1_title': 'Dự đoán tỷ số chung cuộc',
        'dialog_ai_feature_1_desc': 'Xem tỷ số chính xác mà AI dự đoán',
        'dialog_ai_feature_2_title': 'Xác suất chiến thắng',
        'dialog_ai_feature_2_desc': 'Phân tích xác suất Thắng / Hòa / Thua',
        'dialog_ai_feature_3_title': 'Mức độ tin cậy',
        'dialog_ai_feature_3_desc': 'Đánh giá độ tin cậy của AI cho mỗi dự đoán',
        'dialog_ai_btn_later': 'Để sau',
        'dialog_ai_btn_go_premium': 'Nâng cấp Premium'
    },
    'values-ar': {
        'dialog_ai_title': 'شاهد ما يتوقعه الذكاء الاصطناعي',
        'dialog_ai_subtitle': 'افتح توقعات النتائج التفصيلية قبل بدء المباراة.',
        'dialog_ai_feature_1_title': 'النتيجة النهائية المتوقعة',
        'dialog_ai_feature_1_desc': 'شاهد النتيجة الدقيقة التي يتوقعها الذكاء الاصطناعي',
        'dialog_ai_feature_2_title': 'احتمالية الفوز',
        'dialog_ai_feature_2_desc': 'تحليل احتمالية الفوز / التعادل / الخسارة',
        'dialog_ai_feature_3_title': 'مستوى الثقة',
        'dialog_ai_feature_3_desc': 'تقييم ثقة الذكاء الاصطناعي لكل توقع',
        'dialog_ai_btn_later': 'لاحقاً',
        'dialog_ai_btn_go_premium': 'احصل على بريميوم'
    },
    'values-de': {
        'dialog_ai_title': 'Sehen Sie, was die KI vorhersagt',
        'dialog_ai_subtitle': 'Schalten Sie detaillierte Ergebnisprognosen frei, bevor das Spiel beginnt.',
        'dialog_ai_feature_1_title': 'Prognostiziertes Endergebnis',
        'dialog_ai_feature_1_desc': 'Sehen Sie das genaue Ergebnis, das die KI vorhersagt',
        'dialog_ai_feature_2_title': 'Gewinnwahrscheinlichkeit',
        'dialog_ai_feature_2_desc': 'Wahrscheinlichkeitsanalyse für Sieg / Unentschieden / Niederlage',
        'dialog_ai_feature_3_title': 'Konfidenzniveau',
        'dialog_ai_feature_3_desc': 'KI-Konfidenzbewertung für jede Vorhersage',
        'dialog_ai_btn_later': 'Später',
        'dialog_ai_btn_go_premium': 'Premium holen'
    },
    'values-es': {
        'dialog_ai_title': 'Ver lo que predice la IA',
        'dialog_ai_subtitle': 'Desbloquea predicciones detalladas de resultados antes de que comience el partido.',
        'dialog_ai_feature_1_title': 'Resultado final predicho',
        'dialog_ai_feature_1_desc': 'Mira el resultado exacto que predice la IA',
        'dialog_ai_feature_2_title': 'Probabilidad de victoria',
        'dialog_ai_feature_2_desc': 'Análisis de probabilidad de ganar / empatar / perder',
        'dialog_ai_feature_3_title': 'Nivel de confianza',
        'dialog_ai_feature_3_desc': 'Calificación de confianza de la IA para cada predicción',
        'dialog_ai_btn_later': 'Más tarde',
        'dialog_ai_btn_go_premium': 'Hacerse Premium'
    },
    'values-fr': {
        'dialog_ai_title': "Voir ce que l'IA prédit",
        'dialog_ai_subtitle': "Débloquez des prédictions de score détaillées avant le début du match.",
        'dialog_ai_feature_1_title': "Score final prédit",
        'dialog_ai_feature_1_desc': "Voir le score exact prédit par l'IA",
        'dialog_ai_feature_2_title': "Probabilité de victoire",
        'dialog_ai_feature_2_desc': "Analyse de probabilité de victoire / nul / défaite",
        'dialog_ai_feature_3_title': "Niveau de confiance",
        'dialog_ai_feature_3_desc': "Indice de confiance de l'IA pour chaque prédiction",
        'dialog_ai_btn_later': "Plus tard",
        'dialog_ai_btn_go_premium': "Passer Premium"
    },
    'values-hi': {
        'dialog_ai_title': 'देखें कि एआई क्या भविष्यवाणी करता है',
        'dialog_ai_subtitle': 'मैच शुरू होने से पहले विस्तृत स्कोर भविष्यवाणियों को अनलॉक करें।',
        'dialog_ai_feature_1_title': 'भविष्यवाणी की गई अंतिम स्कोर',
        'dialog_ai_feature_1_desc': 'सटीक स्कोर देखें जो एआई भविष्यवाणी करता है',
        'dialog_ai_feature_2_title': 'जीतने की संभावना',
        'dialog_ai_feature_2_desc': 'जीत / ड्रॉ / हार की संभावना का विश्लेषण',
        'dialog_ai_feature_3_title': 'आत्मविश्वास का स्तर',
        'dialog_ai_feature_3_desc': 'प्रत्येक भविष्यवाणी के लिए एआई विश्वास रेटिंग',
        'dialog_ai_btn_later': 'बाद में',
        'dialog_ai_btn_go_premium': 'प्रीमियम प्राप्त करें'
    },
    'values-id': {
        'dialog_ai_title': 'Lihat Apa yang Diprediksi AI',
        'dialog_ai_subtitle': 'Buka kunci prediksi skor terperinci sebelum pertandingan dimulai.',
        'dialog_ai_feature_1_title': 'Prediksi skor akhir',
        'dialog_ai_feature_1_desc': 'Lihat skor pasti yang diprediksi AI',
        'dialog_ai_feature_2_title': 'Probabilitas kemenangan',
        'dialog_ai_feature_2_desc': 'Analisis probabilitas menang / seri / kalah',
        'dialog_ai_feature_3_title': 'Tingkat kepercayaan',
        'dialog_ai_feature_3_desc': 'Peringkat kepercayaan AI untuk setiap prediksi',
        'dialog_ai_btn_later': 'Nanti',
        'dialog_ai_btn_go_premium': 'Dapatkan Premium'
    },
    'values-it': {
        'dialog_ai_title': "Vedi cosa predice l'IA",
        'dialog_ai_subtitle': "Sblocca le previsioni dettagliate del punteggio prima dell'inizio della partita.",
        'dialog_ai_feature_1_title': "Punteggio finale previsto",
        'dialog_ai_feature_1_desc': "Vedi il punteggio esatto che l'IA predice",
        'dialog_ai_feature_2_title': "Probabilità di vittoria",
        'dialog_ai_feature_2_desc': "Analisi della probabilità di vittoria / pareggio / sconfitta",
        'dialog_ai_feature_3_title': "Livello di fiducia",
        'dialog_ai_feature_3_desc': "Valutazione della fiducia dell'IA per ogni previsione",
        'dialog_ai_btn_later': "Più tardi",
        'dialog_ai_btn_go_premium': "Passa a Premium"
    },
    'values-ja': {
        'dialog_ai_title': 'AIの予測を見る',
        'dialog_ai_subtitle': '試合開始前に詳細なスコア予測をアンロックします。',
        'dialog_ai_feature_1_title': '予想最終スコア',
        'dialog_ai_feature_1_desc': 'AIが予測する正確なスコアを表示',
        'dialog_ai_feature_2_title': '勝率',
        'dialog_ai_feature_2_desc': '勝/分/負の確率分析',
        'dialog_ai_feature_3_title': '信頼度',
        'dialog_ai_feature_3_desc': '各予測に対するAIの信頼度評価',
        'dialog_ai_btn_later': '後で',
        'dialog_ai_btn_go_premium': 'プレミアムに移行'
    },
    'values-pt': {
        'dialog_ai_title': 'Veja o que a IA prevê',
        'dialog_ai_subtitle': 'Desbloqueie previsões detalhadas de resultados antes do início da partida.',
        'dialog_ai_feature_1_title': 'Resultado final previsto',
        'dialog_ai_feature_1_desc': 'Veja o resultado exato que a IA prevê',
        'dialog_ai_feature_2_title': 'Probabilidade de vitória',
        'dialog_ai_feature_2_desc': 'Análise de probabilidade de vitória / empate / derrota',
        'dialog_ai_feature_3_title': 'Nível de confiança',
        'dialog_ai_feature_3_desc': 'Avaliação de confiança da IA para cada previsão',
        'dialog_ai_btn_later': 'Mais tarde',
        'dialog_ai_btn_go_premium': 'Obter Premium'
    },
    'values-ru': {
        'dialog_ai_title': 'Посмотрите, что предсказывает ИИ',
        'dialog_ai_subtitle': 'Разблокируйте подробные прогнозы результатов до начала матча.',
        'dialog_ai_feature_1_title': 'Прогнозируемый итоговый счет',
        'dialog_ai_feature_1_desc': 'Посмотрите точный счет, который предсказывает ИИ',
        'dialog_ai_feature_2_title': 'Вероятность победы',
        'dialog_ai_feature_2_desc': 'Анализ вероятности победы / ничьей / поражения',
        'dialog_ai_feature_3_title': 'Уровень уверенности',
        'dialog_ai_feature_3_desc': 'Оценка уверенности ИИ для каждого прогноза',
        'dialog_ai_btn_later': 'Позже',
        'dialog_ai_btn_go_premium': 'Перейти на Premium'
    },
    'values-th': {
        'dialog_ai_title': 'ดูว่า AI คาดการณ์อะไร',
        'dialog_ai_subtitle': 'ปลดล็อกการคาดการณ์คะแนนโดยละเอียดก่อนเริ่มการแข่งขัน',
        'dialog_ai_feature_1_title': 'คาดการณ์คะแนนสุดท้าย',
        'dialog_ai_feature_1_desc': 'ดูคะแนนที่แน่นอนที่ AI คาดการณ์',
        'dialog_ai_feature_2_title': 'ความน่าจะเป็นในการชนะ',
        'dialog_ai_feature_2_desc': 'การวิเคราะห์ความน่าจะเป็น ชนะ / เสมอ / แพ้',
        'dialog_ai_feature_3_title': 'ระดับความมั่นใจ',
        'dialog_ai_feature_3_desc': 'การให้คะแนนความมั่นใจของ AI สำหรับทุกการคาดการณ์',
        'dialog_ai_btn_later': 'ไว้ทีหลัง',
        'dialog_ai_btn_go_premium': 'เป็นพรีเมียม'
    },
    'values-tr': {
        'dialog_ai_title': 'AI\\\'ın Ne Tahmin Ettiğini Görün',
        'dialog_ai_subtitle': 'Maç başlamadan önce detaylı skor tahminlerinin kilidini açın.',
        'dialog_ai_feature_1_title': 'Tahmini maç skoru',
        'dialog_ai_feature_1_desc': 'AI\\\'ın tahmin ettiği tam skoru görün',
        'dialog_ai_feature_2_title': 'Kazanma olasılığı',
        'dialog_ai_feature_2_desc': 'Galibiyet / Beraberlik / Mağlubiyet olasılık analizi',
        'dialog_ai_feature_3_title': 'Güven seviyesi',
        'dialog_ai_feature_3_desc': 'Her tahmin için AI güven derecesi',
        'dialog_ai_btn_later': 'Daha sonra',
        'dialog_ai_btn_go_premium': 'Premium\\\'a Geç'
    },
    'values-ur': {
        'dialog_ai_title': 'دیکھیں کہ اے آئی کیا پیشن گوئی کرتا ہے',
        'dialog_ai_subtitle': 'میچ شروع ہونے سے پہلے تفصیلی اسکور کی پیشن گوئیاں انلاک کریں۔',
        'dialog_ai_feature_1_title': 'متوقع حتمی اسکور',
        'dialog_ai_feature_1_desc': 'اے آئی کی پیش گوئی کردہ درست اسکور دیکھیں',
        'dialog_ai_feature_2_title': 'جیتنے کا امکان',
        'dialog_ai_feature_2_desc': 'جیت / ڈرا / ہار کے امکان کا تجزیہ',
        'dialog_ai_feature_3_title': 'اعتماد کا سطح',
        'dialog_ai_feature_3_desc': 'ہر پیشن گوئی کے لئے اے آئی کا اعتماد کی درجہ بندی',
        'dialog_ai_btn_later': 'بعد میں',
        'dialog_ai_btn_go_premium': 'پریمیم حاصل کریں'
    }
}

# Use relative path inside the workspace
res_path = './app/src/main/res'

for folder, keys in translations.items():
    strings_file = os.path.join(res_path, folder, 'strings.xml')
    if not os.path.exists(strings_file):
        print(f"File not found: {strings_file}")
        continue
    
    with open(strings_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Clean up existing keys to prevent duplicates
    for k in keys.keys():
        pattern = rf'    <string name="{k}">.*?</string>\n?'
        content = re.sub(pattern, '', content)
    
    # Find closing </resources> and append the new keys
    closing_tag_pos = content.rfind('</resources>')
    if closing_tag_pos == -1:
        print(f"Error: No </resources> tag found in {strings_file}")
        continue
    
    xml_to_insert = '\n    <!-- AI Prediction Paywall Dialog -->\n'
    for k, v in keys.items():
        xml_to_insert += f'    <string name="{k}">{v}</string>\n'
    
    new_content = content[:closing_tag_pos] + xml_to_insert + content[closing_tag_pos:]
    
    with open(strings_file, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"Successfully updated {strings_file}")
