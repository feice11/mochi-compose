"""Copy selected original fonts, with source hashes. Chinese uses platform fallback."""
from pathlib import Path
import shutil,hashlib,json
root=Path(__file__).resolve().parents[2]
src=root/'artifacts/nothing-resource-research/native-extracted/system/system/fonts'
out=root/'mochi-nothing/src/main/res/font';out.mkdir(parents=True,exist_ok=True)
records=[]
for source,target in [('Inter-Regular.ttf','nothing_inter.ttf'),('NType82-Headline.otf','nothing_display.otf'),('Ndot-57-Aligned.otf','nothing_dot.otf')]:
    p=src/source;shutil.copyfile(p,out/target)
    records.append({'source':source,'resource':target,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(root/'mochi-nothing/font-provenance.json').write_text(json.dumps({'build':'Metroid_B4.1-260814-1733','display':'Original NType82 Latin; platform Chinese fallback. No Chinese dot font.','sources':records},indent=2)+'\n')
print('Copied',len(records),'original fonts; Chinese uses readable platform fallback')
