#! /bin/sh

cport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Coordinator $cport 3 A B &
sleep 2s
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
sleep 20s
kill -9 `pgrep -af $cport | awk '{print $1}'`

cport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Coordinator $cport 5 A B C &
sleep 2s
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 0 &
pport=`python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'`
java Participant $cport $pport 5000 1 &
sleep 50s
kill -9 `pgrep -af $cport | awk '{print $1}'`

######################################################################
exit
######################################################################
