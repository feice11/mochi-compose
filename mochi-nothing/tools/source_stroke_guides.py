# Copyright 2026, mochi-compose contributors
# SPDX-License-Identifier: Apache-2.0

"""Extract source stroke guides for regular-grid dot selection.

Source silhouettes are never manually redrawn. This is a local derivative, not
Nothing-original artwork. Only the existing unmodified source outlines are used.
"""
from pathlib import Path
import sys,json,math,xml.etree.ElementTree as ET
import numpy as np, cv2
from scipy.ndimage import distance_transform_edt
from scipy.interpolate import splprep,splev
from skimage.morphology import skeletonize
from PIL import Image
import aggdraw
from fontTools.pens.basePen import BasePen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.svgLib.path import parse_path
R=Path(__file__).resolve().parents[2];B=R/'mochi-nothing/tools/vendor/ndot-mdi'
index=json.load(open(B/'index.json'))
class Pen(BasePen):
 def __init__(self,p):super().__init__(None);self.p=p
 def _moveTo(self,p):self.p.moveto(*p)
 def _lineTo(self,p):self.p.lineto(*p)
 def _curveToOne(self,a,b,c):self.p.curveto(*a,*b,*c)
 def _closePath(self):self.p.close()
 def _endPath(self):pass
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform

def build(name,pitch=2.15,radius=.82):
 file=B/'originals'/(name+'.svg');raw=ET.parse(file).getroot().find('{http://www.w3.org/2000/svg}path').get('d');p=BoundsPen(None);parse_path(raw,p);l,t,r,b=p.bounds
 scale=min(1, .84*index[name]['unitsPerEm']/max(r-l,b-t))*25/index[name]['unitsPerEm'];mat=Transform(scale,0,0,-scale,12-scale*(l+r)/2,12+scale*(t+b)/2)
 px=24;N=24*px;image=Image.new('L',(N,N));c=aggdraw.Draw(image);path=aggdraw.Path();parse_path(raw,TransformPen(Pen(path),Transform().scale(px).transform(mat)));c.path(path,aggdraw.Brush('white'));c.flush();mask=np.array(image)>127
 dist=distance_transform_edt(mask)/px;sk=skeletonize(mask);extra=[];primitive=[]
 n,labels,stats,centers=cv2.connectedComponentsWithStats(mask.astype('uint8'),8)
 for lab,(x,y,w,h,area) in enumerate(stats[1:],1):
  comp=(labels==lab).astype('uint8');contours,_=cv2.findContours(comp,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
  hull=cv2.convexHull(np.concatenate(contours));hull_area=cv2.contourArea(hull)
  ys,xs=np.where(sk & (labels==lab))
  if len(xs) and max(np.ptp(xs),np.ptp(ys))/px<.45:
   primitive.append((np.array([centers[lab][0]/px,centers[lab][1]/px]),min(1.6,max(radius,float(dist[labels==lab].max())*.8))))
   sk[labels==lab]=False
  # A filled capsule/pencil needs its contour, not a centerline that erases its body.
  if name in {'microphone','pencil'} and hull_area and area/hull_area>.88 and min(w,h)/px>2.8:
   eroded=cv2.erode(comp,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(2*round(radius*px)+1,)*2))
   cs,_=cv2.findContours(eroded,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
   for c in cs:
    pts=c[:,0,:].astype(float)/px;pts=np.vstack([pts,pts[:1]]);extra.append(pts)
   sk[labels==lab]=False
 nodes=set(map(tuple,np.argwhere(sk)));isolated=[];g={}
 for y,x in nodes:
  near=[]
  for dy in [-1,0,1]:
   for dx in [-1,0,1]:
    if not (dx or dy) or (y+dy,x+dx) not in nodes:continue
    if dx and dy and ((y,x+dx) in nodes or (y+dy,x) in nodes):continue
    near.append((y+dy,x+dx))
  g[(y,x)]=near
 # Remove raster spurs shorter than .65dp at stroke corners, not real short arms.
 for _ in range(3):
  gone=set()
  for start in list(g):
   if len(g[start])!=1:continue
   chain=[start];prev=start;cur=g[start][0];length=math.dist(start,cur)/px
   while len(g.get(cur,[]))==2:
    nxt=next(n for n in g[cur] if n!=prev);chain.append(cur);prev,cur=cur,nxt;length+=math.dist(prev,cur)/px
   if len(g.get(cur,[]))>2 and length<.65:gone.update(chain)
  if not gone:break
  for p in gone:g.pop(p,None)
  for p in g:g[p]=[q for q in g[p] if q not in gone]
 visited=set();paths=[];closures=[]
 for point,near in g.items():
  if not near:isolated.append((np.array([point[1]/px,point[0]/px]),min(1.45,max(radius,dist[point]*.8))))
 for pts in extra:
  cum=np.r_[0,np.cumsum(np.linalg.norm(np.diff(pts,axis=0),axis=1))];values=np.linspace(0,cum[-1],max(5,round(cum[-1]/pitch)),endpoint=False);paths.append(np.c_[np.interp(values,cum,pts[:,0]),np.interp(values,cum,pts[:,1])]);closures.append(True)
 starts=sorted(g,key=lambda p:(len(g[p])==2,p))
 for p in starts:
  for q in g[p]:
   edge=tuple(sorted((p,q)))
   if edge in visited:continue
   chain=[p];prev,cur=p,q;visited.add(edge)
   while True:
    chain.append(cur)
    if len(g[cur])!=2 or cur==p:break
    nxt=next(n for n in g[cur] if n!=prev);visited.add(tuple(sorted((cur,nxt))));prev,cur=cur,nxt
   pts=np.array([(x/px,y/px) for y,x in chain]);length=np.linalg.norm(np.diff(pts,axis=0),axis=1).sum()
   if length<.24:continue
   # Smooth only raster noise. The original source centerline owns the shape.
   simplified=cv2.approxPolyDP(pts.astype('float32'),.055,False).reshape(-1,2)
   closed=np.linalg.norm(pts[-1]-pts[0])<.1
   paths.append(simplified);closures.append(closed)

 return paths,closures,primitive+isolated,mask
