/** Local Chromium integration tests. Uses a fresh generated profile and mock TV, never a user's browser session. */
import {createServer} from 'node:http';
import {readFile,writeFile,mkdir,mkdtemp} from 'node:fs/promises';
import {spawn} from 'node:child_process';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const out=resolve(root,'build/companion-browser');await mkdir(out,{recursive:true});
const page=await readFile(resolve(root,'assets/phone_companion.html'));
const state={ready:true,authorized:true,keys:[],delay:0};let browser,ws;
const server=createServer(async(req,res)=>{
 let body='';for await(const chunk of req)body+=chunk;
 const url=new URL(req.url,'http://localhost');
 res.setHeader('Cache-Control','no-store');
 if(url.pathname==='/'){res.setHeader('Content-Type','text/html; charset=utf-8');res.end(page);return;}
 res.setHeader('Content-Type','application/json');
 if(url.pathname==='/pair'){state.authorized=true;res.end(JSON.stringify({token:'local-preview-only'}));return;}
 if(!state.authorized||req.headers.authorization!=='Bearer local-preview-only'){res.statusCode=401;res.end(JSON.stringify({error:'Session expired'}));return;}
 if(url.pathname==='/api/ping'){res.end(JSON.stringify({ok:true,launcherReady:state.ready}));return;}
 if(url.pathname==='/api/apps'){res.end(JSON.stringify([{label:'Sample TV app',package:'example.tv'},{label:'Sample game',package:'example.game'}]));return;}
 if(url.pathname==='/api/key'){state.keys.push(JSON.parse(body).key);if(state.delay){const delay=state.delay;state.delay=0;await new Promise(r=>setTimeout(r,delay));}res.end('{"ok":true}');return;}
 res.end('{"ok":true}');
});
await new Promise(r=>server.listen(0,'127.0.0.1',r));
const origin='http://127.0.0.1:'+server.address().port;
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let sequence=0;const pending=new Map(),errors=[];const results=[];
function check(value,message){assert.ok(value,message);results.push(message);}
function call(method,params={}){return new Promise((resolve,reject)=>{const id=++sequence;const timer=setTimeout(()=>{pending.delete(id);reject(Error('CDP timeout: '+method));},15000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const result=await call('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(result.exceptionDetails)throw Error(result.exceptionDetails.text+': '+(result.exceptionDetails.exception?.description||''));return result.result.value;}
async function until(expression){for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100);}throw Error('Page condition timed out: '+expression);}
async function screenshot(name){const result=await call('Page.captureScreenshot',{format:'png',captureBeyondViewport:false});await writeFile(resolve(out,name),Buffer.from(result.data,'base64'));}
try {
 const profile=await mkdtemp(resolve(out,'profile-'));
 const executable=process.env.DETOX_TEST_BROWSER||'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
 browser=spawn(executable,['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--remote-debugging-address=127.0.0.1','--remote-debugging-port=0','--user-data-dir='+profile,'about:blank'],{windowsHide:true,stdio:'ignore'});
 let port;
 for(let i=0;i<120;i++){try{port=Number((await readFile(resolve(profile,'DevToolsActivePort'),'utf8')).split('\n')[0]);break;}catch{}await sleep(100);}
 assert.ok(port,'Headless browser did not start');
 const target=await (await fetch('http://127.0.0.1:'+port+'/json/new?about:blank',{method:'PUT'})).json();
 ws=new WebSocket(target.webSocketDebuggerUrl);
 await new Promise((r,j)=>{ws.addEventListener('open',r,{once:true});ws.addEventListener('error',j,{once:true});});
 ws.addEventListener('message',event=>{const data=JSON.parse(event.data);if(data.id){const waiter=pending.get(data.id);if(waiter){pending.delete(data.id);clearTimeout(waiter.timer);data.error?waiter.reject(Error(data.error.message)):waiter.resolve(data.result);}}else if(data.method==='Runtime.exceptionThrown')errors.push(data.params.exceptionDetails.text);});
 await call('Page.enable');await call('Runtime.enable');
 await call('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
 await call('Page.navigate',{url:origin+'/?pin=123456'});
 await until(`document.getElementById('connectionBadge')?.textContent==='Connected'`);
 check(await evaluate(`!document.getElementById('controls').classList.contains('hidden')`),'QR pairing reaches usable controls');
 check(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'390 px mobile has no horizontal overflow');
 check(await evaluate(`document.querySelector('[data-key="up"]').getBoundingClientRect().width>=70`),'thumb-sized D-pad target');
 const commandsBefore=state.keys.length;
 check(await evaluate(`sendRemoteKey('right','Right')`),'direction request acknowledged by mock TV');
 check(state.keys.at(-1)==='right'&&state.keys.length===commandsBefore+1,'single button sends one request');
 const rect=await evaluate(`(()=>{const r=document.querySelector('[data-key="down"]').getBoundingClientRect();return{x:r.x+r.width/2,y:r.y+r.height/2}})()`);
 const holdStart=state.keys.length;
 await call('Input.dispatchMouseEvent',{type:'mousePressed',x:rect.x,y:rect.y,button:'left',clickCount:1});await sleep(660);
 await call('Input.dispatchMouseEvent',{type:'mouseReleased',x:rect.x,y:rect.y,button:'left',clickCount:1});await sleep(350);
 const holdEnd=state.keys.length;check(holdEnd-holdStart>=2&&holdEnd-holdStart<=5,'held arrow repeats at a bounded rate');
 await sleep(400);check(state.keys.length===holdEnd,'release stops held navigation');
 state.delay=1000;const queueStart=state.keys.length;
 await evaluate(`Promise.all(Array.from({length:6},()=>sendRemoteKey('left','Left')))`);
 check(state.keys.length===queueStart+1,'stale queued keys are dropped rather than replayed');
 state.ready=false;await evaluate('reconnect()');
 check(await evaluate(`document.querySelector('[data-key="up"]').disabled&&!document.querySelector('[data-key="home"]').disabled`),'external app state disables Detox-only navigation but preserves Home');
 state.ready=true;await evaluate('reconnect()');
 await evaluate(`document.querySelector('[data-tab="apps"]').click()`);await until(`document.querySelectorAll('#apps button').length===2`);
 check(await evaluate(`!document.getElementById('appsPanel').classList.contains('hidden')`),'Apps tab displays actual response');
 await evaluate(`document.getElementById('filter').value='game';document.getElementById('filter').dispatchEvent(new Event('input'))`);
 check(await evaluate(`document.querySelectorAll('#apps button:not(.hidden)').length===1`),'app filter operates');
 await evaluate(`document.querySelector('[data-tab="share"]').click()`);
 check(await evaluate(`!document.getElementById('videoPanel').closest('[data-companion-view]').classList.contains('hidden')`),'media transfer controls remain reachable');
 await evaluate(`document.querySelector('[data-tab="remote"]').click()`);await screenshot('remote-mobile.png');
 await call('Emulation.setDeviceMetricsOverride',{width:1280,height:900,deviceScaleFactor:1,mobile:false});
 check(await evaluate('document.documentElement.scrollWidth<=innerWidth'),'desktop has no horizontal overflow');await screenshot('remote-desktop.png');
 state.authorized=false;await evaluate('reconnect()');
 check(await evaluate(`!document.getElementById('pairCard').classList.contains('hidden')&&document.getElementById('controls').classList.contains('hidden')`),'expired session returns to pairing');
 await call('Page.addScriptToEvaluateOnNewDocument',{source:`Object.defineProperty(window,'sessionStorage',{get(){throw new DOMException('Blocked storage','SecurityError')}});`});
 await call('Page.navigate',{url:origin+'/?pin=123456'});await until(`document.getElementById('connectionBadge')?.textContent==='Connected'`);
 check(await evaluate(`!document.getElementById('controls').classList.contains('hidden')`),'restricted sessionStorage still permits pairing and remote use');
 check(errors.length===0,'no uncaught browser exceptions');
 await writeFile(resolve(out,'results.json'),JSON.stringify({checks:results.length,results,browser:'installed Edge, fresh generated profile',mockTV:true},null,2));
 console.log('Companion browser: '+results.length+' checks passed; mobile/desktop screenshots saved.');
} finally {
 if(ws?.readyState===WebSocket.OPEN){try{await call('Browser.close');}catch{}ws.close();}
 browser?.kill();server.closeAllConnections();await new Promise(r=>server.close(r));
}
