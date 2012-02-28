echo "#PBS -N $1
#PBS -l nodes=1:ppn=1
#PBS -p 0
#PBS -j oe
#PBS -m b
#PBS -m e
#PBS -m a
#PBS -V
#PBS -o out/$1.out
#PBS -e err/$1.err
#PBS -M thomas.ritter@uzh.ch
"

echo 'workingDir=/home/torque/tmp/${USER}.${PBS_JOBID}

cp -r ~/jppf-node $workingDir/jppf-node
cd $workingDir/jppf-node

chmod +x startNode.sh

cmd="./startNode.sh $2"
$cmd'
