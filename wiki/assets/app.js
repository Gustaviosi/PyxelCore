let LANG="pt",PAGES=[],CURRENT="",searchTimer=null;
function renderSidebar(filter){
  const sb=document.getElementById("sidebar"),groups={};
  let src=PAGES;
  if(filter){const q=filter.toLowerCase();src=PAGES.filter(p=>p.title.toLowerCase().includes(q)||(p.tags||[]).some(t=>t.toLowerCase().includes(q))||p.html.toLowerCase().includes(q));}
  for(let i=0;i<src.length;i++){const p=src[i];if(!groups[p.group])groups[p.group]=[];groups[p.group].push(p);}
  const parts=[];
  for(const g in groups){parts.push("<h3>"+g+"</h3>");const arr=groups[g];for(let i=0;i<arr.length;i++)parts.push('<a href="#/'+arr[i].id+'" data-id="'+arr[i].id+'">'+arr[i].title+'</a>');}
  sb.innerHTML=parts.join("");
}
function renderPage(id){
  if(id===CURRENT)return;
  const p=PAGES.find(x=>x.id===id);if(!p)return;
  CURRENT=p.id;
  document.getElementById("content").innerHTML=p.html;
  const links=document.querySelectorAll("#sidebar a");
  for(let i=0;i<links.length;i++)links[i].classList.toggle("active",links[i].dataset.id===p.id);
  window.scrollTo(0,0);
}
function setLang(lang){
  LANG=lang;document.documentElement.lang=lang;
  const btns=document.querySelectorAll(".langs button");
  for(let i=0;i<btns.length;i++)btns[i].classList.toggle("on",btns[i].dataset.lang===lang);
  PAGES=(lang==="pt"||!window.CONTENT_EN.pages.length)?window.CONTENT_PT.pages:window.CONTENT_EN.pages;
  CURRENT="";renderSidebar();
  const hash=location.hash.replace(/^#\//,"");
  renderPage(hash||PAGES[0].id);
}
function onSearchInput(e){const v=e.target.value;if(searchTimer)clearTimeout(searchTimer);searchTimer=setTimeout(()=>renderSidebar(v),200);}
function init(){
  const btns=document.querySelectorAll(".langs button");
  for(let i=0;i<btns.length;i++)btns[i].addEventListener("click",function(){setLang(this.dataset.lang)});
  window.addEventListener("hashchange",function(){const id=location.hash.replace(/^#\//,"");if(id)renderPage(id);});
  document.getElementById("search").addEventListener("input",onSearchInput);
  document.addEventListener("keydown",function(e){if((e.ctrlKey||e.metaKey)&&e.key==="k"){e.preventDefault();document.getElementById("search").focus()}});
  setLang("pt");
}
document.addEventListener("DOMContentLoaded",init);